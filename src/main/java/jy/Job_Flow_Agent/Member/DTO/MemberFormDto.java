package jy.Job_Flow_Agent.Member.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jy.Job_Flow_Agent.Member.Entity.Member;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "MemberFormDto", description = "회원 가입 및 수정 요청에 사용되는 입력 모델")
public class MemberFormDto {
    @Schema(description = "로그인 아이디", example = "fituser123")
    @NotBlank(message = "아이디를 입력해주세요")
    @Size(min = 4, max = 20, message = "아이디는 4-20자 사이여야 합니다")
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "아이디는 소문자, 숫자, _, -만 사용 가능합니다")
    private String username;

    @Schema(description = "회원 이메일 주소", example = "user@example.com")
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "서비스 내에서 사용하는 표시 이름", example = "길동이")
    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(min = 2, max = 15, message = "닉네임은 2-15자 사이여야 합니다")
    private String displayName;

    @Schema(description = "로그인 비밀번호", example = "S3cureP@ssw0rd", nullable = true)
    private String password;

    @Schema(description = "회원 연락처", example = "010-1234-5678", nullable = true)
    private String phone;

    @Schema(description = "회원 성별", example = "FEMALE", nullable = true)
    private String sex;

    @Schema(description = "회원 나이", example = "28", nullable = true)
    private Integer age;

    @Schema(description = "회원 키(cm)", example = "165.0")
    private double height;

    @Schema(description = "회원 몸무게(kg)", example = "55.0")
    private double weight;

    @Schema(description = "부여할 권한 목록", example = "[\"ROLE_USER\"]", nullable = true)
    private Set<String> roles;

    @Schema(description = "프리미엄 구독 여부", example = "false")
    private boolean isPremium;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    private boolean privacyAccepted;

    @Schema(description = "이용 약관 동의 여부", example = "true")
    private boolean termsAccepted;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "false")
    private boolean marketingAccepted;

    public Member convertToMember() {
        return Member.builder()
                .username(this.username)
                .password(this.password)
                .email(this.email)
                .displayName(this.displayName)
                .phone(this.phone)
                .sex(this.sex)
                .age(this.age)
                .height(this.height)
                .weight(this.weight)
                .privacyAccepted(this.privacyAccepted)
                .termsAccepted(this.termsAccepted)
                .marketingAccepted(this.marketingAccepted)
                .build();
    }

    public void updateMember(Member member) {
        member.setDisplayName(this.displayName);
        member.setEmail(this.email);
        member.setPhone(this.phone);
        member.setSex(this.sex);
        member.setAge(this.age);
        member.setHeight(this.height);
        member.setWeight(this.weight);
        member.setMarketingAccepted(this.marketingAccepted);
        // password는 따로 처리하므로 여기선 제외
    }

    public void adminUpdateMember(Member member) {
        member.setDisplayName(this.displayName);
        member.setEmail(this.email);
        member.setPhone(this.phone);
        member.setSex(this.sex);
        member.setAge(this.age);
        member.setHeight(this.height);
        member.setWeight(this.weight);
        member.setRoles(this.roles);
        member.setPremium(this.isPremium);
        member.setMarketingAccepted(this.marketingAccepted);
        // password는 따로 처리하므로 여기선 제외
    }

}