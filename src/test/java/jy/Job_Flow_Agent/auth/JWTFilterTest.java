package jy.Job_Flow_Agent.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import java.util.HashMap;
import java.util.Map;
import jy.Job_Flow_Agent.Auth.JWT.JWTFilter;
import jy.Job_Flow_Agent.Auth.JWT.JWTUtil;
import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWTFilter 단위 테스트")
class JWTFilterTest {

    @Mock
    private JWTUtil jwtUtil;

    private JWTFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JWTFilter(jwtUtil);
        ReflectionTestUtils.setField(jwtFilter, "appEnv", "development");
        ReflectionTestUtils.setField(jwtFilter, "cookieDomain", "localhost");
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────
    //  유효한 Bearer 토큰 → SecurityContext에 인증 정보 설정
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("JF-01: 유효한 Bearer 토큰 → SecurityContext에 인증 정보 설정")
    void validBearerToken_setsAuthentication() throws Exception {
        // given
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        Claims claims = buildClaims("testuser", Set.of("ROLE_USER"));
        given(jwtUtil.isTokenExpired(token)).willReturn(false);
        given(jwtUtil.extractClaims(token)).willReturn(claims);

        // when
        jwtFilter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
    }

    // ─────────────────────────────────────────────────────
    //  토큰 없는 요청 → SecurityContext 비어있음, 필터 통과
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("JF-02: 토큰 없는 요청 → SecurityContext 비어있음, 필터 통과")
    void noToken_doesNotSetAuthentication_andPassesChain() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        jwtFilter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(jwtUtil).should(never()).isTokenExpired(org.mockito.ArgumentMatchers.anyString());
    }

    // ─────────────────────────────────────────────────────
    //  만료된 토큰 → 401 반환
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("JF-03: 만료된 토큰 → 401 Unauthorized 응답")
    void expiredToken_returns401() throws Exception {
        // given
        String token = "expired.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        given(jwtUtil.isTokenExpired(token)).willReturn(true);

        // when
        jwtFilter.doFilter(request, response, chain);

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Access token expired");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ─────────────────────────────────────────────────────
    //  제외 경로 → JWT 검증 없이 통과
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("JF-04: 제외 경로(/api/v1/auth/login) → shouldNotFilter = true, JWT 검증 없음")
    void excludedPath_shouldNotFilter() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");

        // when - shouldNotFilter()는 직접 호출 가능 (protected → 테스트 패키지 동일 불가, 리플렉션)
        boolean shouldSkip = (boolean) ReflectionTestUtils.invokeMethod(jwtFilter, "shouldNotFilter", request);

        // then
        assertThat(shouldSkip).isTrue();
    }

    @Test
    @DisplayName("JF-04: /api/v1/auth/** 전체가 제외 경로에 해당")
    void excludedPathPattern_authAll_shouldNotFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/refresh-token");

        boolean shouldSkip = (boolean) ReflectionTestUtils.invokeMethod(jwtFilter, "shouldNotFilter", request);

        assertThat(shouldSkip).isTrue();
    }

    // ─────────────────────────────────────────────────────
    //  쿠키에서 Access Token 추출 → SecurityContext 설정
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("JF-05: 쿠키의 accessToken → SecurityContext에 인증 정보 설정")
    void cookieAccessToken_setsAuthentication() throws Exception {
        // given
        String token = "cookie.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("accessToken", token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        Claims claims = buildClaims("cookieuser", Set.of("ROLE_USER"));
        given(jwtUtil.isTokenExpired(token)).willReturn(false);
        given(jwtUtil.extractClaims(token)).willReturn(claims);

        // when
        jwtFilter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("cookieuser");
    }

    // ─────────────────────────────────────────────────────
    //  헬퍼: userInfo 클레임을 포함한 Claims 생성
    // ─────────────────────────────────────────────────────
    private Claims buildClaims(String username, Set<String> roles) throws Exception {
        String roleStr = roles.stream()
                .map(r -> "\"" + r + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        String userInfoJson = String.format(
                "{\"id\":1,\"username\":\"%s\",\"displayName\":\"테스트\",\"roleSet\":[%s]}",
                username, roleStr
        );
        // jjwt 0.12.x: no-arg 생성자 및 setSubject() 제거됨 → Map 생성자 사용
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(Claims.SUBJECT, username); // "sub"
        claimsMap.put("userInfo", userInfoJson);
        return new DefaultClaims(claimsMap);
    }
}
