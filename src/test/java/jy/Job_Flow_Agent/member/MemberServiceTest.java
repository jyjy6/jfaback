package jy.Job_Flow_Agent.member;

import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.DTO.MemberDto;
import jy.Job_Flow_Agent.Member.DTO.MemberFormDto;
import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Repository.MemberRepository;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import jy.Job_Flow_Agent.Member.Service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private MemberFormDto validFormDto(String username) {
        return MemberFormDto.builder()
                .username(username)
                .email(username + "@test.com")
                .displayName("닉네임_" + username)
                .password("rawPassword123!")
                .termsAccepted(true)
                .privacyAccepted(true)
                .build();
    }

    private CustomUserDetails customUserDetails(String username) {
        Member member = Member.builder()
                .id(1L)
                .username(username)
                .password("encoded-pw")
                .email(username + "@test.com")
                .displayName("닉네임_" + username)
                .roles(Set.of("ROLE_USER"))
                .build();
        return new CustomUserDetails(member);
    }

    // ─────────────────────────────────────────────────
    //  MS-01: 정상 회원가입
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MS-01: 정상 회원가입 - save 호출, 비밀번호 인코딩 확인")
    void registerUser_success_savesEncodedPassword() {
        // given
        MemberFormDto dto = validFormDto("newuser");
        given(memberRepository.existsByUsername("newuser")).willReturn(false);
        given(memberRepository.existsByDisplayName("닉네임_newuser")).willReturn(false);
        given(memberRepository.existsByEmail("newuser@test.com")).willReturn(false);
        given(passwordEncoder.encode("rawPassword123!")).willReturn("$2a$encodedPw");
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Member saved = memberService.registerUser(dto);

        // then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        then(memberRepository).should().save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$encodedPw");
        assertThat(captor.getValue().getUsername()).isEqualTo("newuser");
    }

    // ─────────────────────────────────────────────────
    //  MS-02: 중복 username
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MS-02: 중복 username 회원가입 → GlobalException(USERNAME_ALREADY_EXISTS)")
    void registerUser_duplicateUsername_throwsException() {
        MemberFormDto dto = validFormDto("existing");
        given(memberRepository.existsByUsername("existing")).willReturn(true);

        assertThatThrownBy(() -> memberService.registerUser(dto))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("USERNAME_ALREADY_EXISTS"));

        then(memberRepository).should(org.mockito.Mockito.never()).save(any());
    }

    // ─────────────────────────────────────────────────
    //  MS-03: 중복 email
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MS-03: 중복 email 회원가입 → GlobalException(EMAIL_ALREADY_EXISTS)")
    void registerUser_duplicateEmail_throwsException() {
        MemberFormDto dto = validFormDto("user2");
        given(memberRepository.existsByUsername("user2")).willReturn(false);
        given(memberRepository.existsByDisplayName("닉네임_user2")).willReturn(false);
        given(memberRepository.existsByEmail("user2@test.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.registerUser(dto))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS"));
    }

    // ─────────────────────────────────────────────────
    //  MS-04: 중복 displayName
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MS-04: 중복 displayName 회원가입 → GlobalException(DISPLAYNAME_ALREADY_EXISTS)")
    void registerUser_duplicateDisplayName_throwsException() {
        MemberFormDto dto = validFormDto("user3");
        given(memberRepository.existsByUsername("user3")).willReturn(false);
        given(memberRepository.existsByDisplayName("닉네임_user3")).willReturn(true);

        assertThatThrownBy(() -> memberService.registerUser(dto))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("DISPLAYNAME_ALREADY_EXISTS"));
    }

    // ─────────────────────────────────────────────────
    //  MS-05: 회원 정보 정상 조회
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MS-05: 존재하는 사용자 getUserInfo() → MemberDto 반환, 필드 일치")
    void getUserInfo_existingMember_returnsMemberDto() {
        // given
        Member member = Member.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@test.com")
                .displayName("테스트닉")
                .password("encoded-pw")
                .roles(Set.of("ROLE_USER"))
                .build();
        given(memberRepository.findByUsername("testuser")).willReturn(Optional.of(member));
        CustomUserDetails userDetails = customUserDetails("testuser");

        // when
        MemberDto result = memberService.getUserInfo(userDetails);

        // then
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("testuser@test.com");
        assertThat(result.getDisplayName()).isEqualTo("테스트닉");
    }

    // ─────────────────────────────────────────────────
    //  MS-06: 존재하지 않는 회원 조회
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MS-06: 존재하지 않는 사용자 getUserInfo() → GlobalException(MEMBER_NOT_FOUND, 404)")
    void getUserInfo_nonExistentMember_throwsException() {
        given(memberRepository.findByUsername("ghost")).willReturn(Optional.empty());
        CustomUserDetails userDetails = customUserDetails("ghost");

        assertThatThrownBy(() -> memberService.getUserInfo(userDetails))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> {
                    GlobalException ge = (GlobalException) ex;
                    assertThat(ge.getErrorCode()).isEqualTo("MEMBER_NOT_FOUND");
                    assertThat(ge.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ─────────────────────────────────────────────────
    //  추가: 비밀번호 미입력 회원가입
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MS-추가: 비밀번호 null 회원가입 → GlobalException(PASSWORD_REQUIRED)")
    void registerUser_nullPassword_throwsException() {
        MemberFormDto dto = validFormDto("user4");
        dto.setPassword(null);
        given(memberRepository.existsByUsername("user4")).willReturn(false);
        given(memberRepository.existsByDisplayName("닉네임_user4")).willReturn(false);
        given(memberRepository.existsByEmail("user4@test.com")).willReturn(false);

        assertThatThrownBy(() -> memberService.registerUser(dto))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("PASSWORD_REQUIRED"));
    }
}
