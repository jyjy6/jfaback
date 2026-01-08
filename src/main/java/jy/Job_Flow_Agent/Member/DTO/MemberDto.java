package jy.Job_Flow_Agent.Member.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jy.Job_Flow_Agent.Member.Entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(name = "MemberDto", description = "회원 정보를 표현하는 응답 모델")
public class MemberDto {
    @Schema(description = "회원 고유 식별자", example = "1")
    private Long id;

    @Schema(description = "로그인 아이디", example = "fitUser123")
    private String username;

    @Schema(description = "회원 이름", example = "홍길동", nullable = true)
    private String name;

    @Schema(description = "서비스 내에서 사용하는 표시 이름", example = "길동이")
    private String displayName;

    @Schema(description = "회원 이메일 주소", example = "user@example.com")
    private String email;

    @Schema(description = "회원 연락처", example = "010-1234-5678", nullable = true)
    private String phone;

    @Schema(description = "회원 키(cm)", example = "175.5", nullable = true)
    private Double height;

    @Schema(description = "회원 몸무게(kg)", example = "68.2", nullable = true)
    private Double weight;

    @Schema(description = "회원 성별", example = "MALE", nullable = true)
    private String sex;

    @Schema(description = "회원 나이", example = "29", nullable = true)
    private Integer age;

    @Schema(description = "계정 생성 일시", example = "2024-01-10T10:15:30")
    private LocalDateTime createdAt;

    @Schema(description = "계정 수정 일시", example = "2024-11-05T09:05:10")
    private LocalDateTime updatedAt;

    @Schema(description = "마지막 로그인 일시", example = "2024-11-10T21:12:45", nullable = true)
    private LocalDateTime lastLogin;

    @Schema(description = "프리미엄 구독 여부", example = "true")
    @JsonProperty("isPremium")
    private boolean isPremium;

    @Schema(description = "프리미엄 구독 만료 일시", example = "2025-01-10T00:00:00", nullable = true)
    private LocalDateTime premiumExpiryDate;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "false")
    private boolean marketingAccepted;

    @Schema(description = "회원 권한 목록", example = "[\"ROLE_USER\", \"ROLE_PREMIUM\"]")
    private Set<String> roleSet;



    public static MemberDto convertToDetailMemberDto(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .username(member.getUsername())
                .displayName(member.getDisplayName())
                .email(member.getEmail())
                .age(member.getAge())
                .sex(member.getSex())
                .phone(member.getPhone())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .isPremium(member.isPremium())
                .lastLogin(member.getLastLogin())
                .premiumExpiryDate(member.getPremiumExpiryDate())
                .marketingAccepted(member.isMarketingAccepted())
                .roleSet(member.getRoles())
                .build();
    }


}