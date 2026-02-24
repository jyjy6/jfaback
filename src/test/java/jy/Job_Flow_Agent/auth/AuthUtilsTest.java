package jy.Job_Flow_Agent.auth;

import jy.Job_Flow_Agent.Auth.Util.AuthUtils;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AuthUtils 단위 테스트")
class AuthUtilsTest {

    private CustomUserDetails buildUserDetails(Long id) {
        Member member = Member.builder()
                .id(id)
                .username("user" + id)
                .password("pw")
                .email("user" + id + "@test.com")
                .displayName("닉네임" + id)
                .roles(Set.of("ROLE_USER"))
                .build();
        return new CustomUserDetails(member);
    }

    // ─────────────────────────────────────────────────
    //  AU-01: 로그인된 상태 loginCheck() - 예외 없음
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("AU-01: 유효한 userDetails로 loginCheck() - 예외 없이 true 반환")
    void loginCheck_validUserDetails_returnsTrue() {
        CustomUserDetails userDetails = buildUserDetails(1L);

        boolean result = AuthUtils.loginCheck(userDetails);

        assertThat(result).isTrue();
    }

    // ─────────────────────────────────────────────────
    //  AU-02: 비로그인 상태 loginCheck() - GlobalException
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("AU-02: userDetails = null → GlobalException(LOGIN_REQUIRED, 401)")
    void loginCheck_null_throwsGlobalException() {
        assertThatThrownBy(() -> AuthUtils.loginCheck(null))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("로그인이 필요합니다")
                .satisfies(ex -> {
                    GlobalException ge = (GlobalException) ex;
                    assertThat(ge.getErrorCode()).isEqualTo("LOGIN_REQUIRED");
                    assertThat(ge.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("AU-02: id = null인 userDetails → GlobalException(LOGIN_REQUIRED, 401)")
    void loginCheck_nullId_throwsGlobalException() {
        // id가 null인 Member로 CustomUserDetails 생성
        Member member = Member.builder()
                .username("ghost")
                .password("pw")
                .email("ghost@test.com")
                .displayName("고스트")
                .roles(Set.of("ROLE_USER"))
                .build(); // id 설정 안 함 → null
        CustomUserDetails userDetails = new CustomUserDetails(member);

        assertThatThrownBy(() -> AuthUtils.loginCheck(userDetails))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("LOGIN_REQUIRED"));
    }

    // ─────────────────────────────────────────────────
    //  AU-03: 본인 ID로 validateMemberId() - 예외 없음
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("AU-03: requestId == 본인 id → 예외 없음")
    void validateMemberId_sameId_noException() {
        CustomUserDetails userDetails = buildUserDetails(42L);

        assertThatCode(() -> AuthUtils.validateMemberId(42L, userDetails))
                .doesNotThrowAnyException();
    }

    // ─────────────────────────────────────────────────
    //  AU-04: 타인 ID로 validateMemberId() - GlobalException
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("AU-04: requestId != 본인 id → GlobalException(USER_INFORM_INCORRECT, 403)")
    void validateMemberId_differentId_throwsGlobalException() {
        CustomUserDetails userDetails = buildUserDetails(1L);

        assertThatThrownBy(() -> AuthUtils.validateMemberId(999L, userDetails))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("유저정보가 다릅니다")
                .satisfies(ex -> {
                    GlobalException ge = (GlobalException) ex;
                    assertThat(ge.getErrorCode()).isEqualTo("USER_INFORM_INCORRECT");
                    assertThat(ge.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}
