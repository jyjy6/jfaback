package jy.Job_Flow_Agent.Member.Service;



import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.DTO.MemberDto;
import jy.Job_Flow_Agent.Member.DTO.MemberFormDto;
import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    public MemberDto getUserInfo(CustomUserDetails customUserDetails) {

        String username = customUserDetails.getUsername();
        // 사용자 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalException("사용자를 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        // DTO 변환
        MemberDto memberDto = new MemberDto();
        return memberDto.convertToDetailMemberDto(member);
    }


    public Member registerUser(MemberFormDto memberFormDto) {

        memberFormCheck(memberFormDto, null);

        // 비밀번호 NULL체크 후 암호화
        if (memberFormDto.getPassword() == null || memberFormDto.getPassword().isEmpty()) {
            throw new GlobalException("비밀번호는 필수 입력 항목입니다", "PASSWORD_REQUIRED");
        }
        memberFormDto.setPassword(passwordEncoder.encode(memberFormDto.getPassword()));

        Member newMember = memberFormDto.convertToMember();

        // 사용자 저장
        Member savedMember = memberRepository.save(newMember);
        return savedMember;
    }










    /**
     * 회원가입 폼 아이디/닉네임/이메일 중복 체크.
     * @param memberFormDto 회원가입/수정 폼
     * @param currentMemberId 값이 NULL일시 회원"가입", 값이 있을시 회원"수정"
     * @return 중복일시 GlobalExecption 던짐
     * */
    private void memberFormCheck(MemberFormDto memberFormDto, Long currentMemberId){
        // 수정 시에는 본인의 기존 데이터는 중복체크에서 제외
        if (currentMemberId != null) {
            if (memberRepository.existsByUsernameAndIdNot(memberFormDto.getUsername(), currentMemberId)) {
                throw new GlobalException("이미 사용 중인 아이디입니다", "USERNAME_ALREADY_EXISTS");
            }
            if (memberRepository.existsByDisplayNameAndIdNot(memberFormDto.getDisplayName(), currentMemberId)) {
                throw new GlobalException("이미 사용 중인 닉네임입니다", "DISPLAYNAME_ALREADY_EXISTS");
            }
            if (memberRepository.existsByEmailAndIdNot(memberFormDto.getEmail(), currentMemberId)) {
                throw new GlobalException("이미 사용 중인 이메일입니다", "EMAIL_ALREADY_EXISTS");
            }
        } else {
            if (memberRepository.existsByUsername(memberFormDto.getUsername())) {
                throw new GlobalException("이미 사용 중인 아이디입니다", "USERNAME_ALREADY_EXISTS");
            }
            if (memberRepository.existsByDisplayName(memberFormDto.getDisplayName())) {
                throw new GlobalException("이미 사용 중인 닉네임입니다", "DISPLAYNAME_ALREADY_EXISTS");
            }
            if (memberRepository.existsByEmail(memberFormDto.getEmail())) {
                throw new GlobalException("이미 사용 중인 이메일입니다", "EMAIL_ALREADY_EXISTS");
            }
        }
    }

}
