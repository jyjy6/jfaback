package jy.Job_Flow_Agent.auth;

import io.jsonwebtoken.Claims;
import jy.Job_Flow_Agent.Auth.JWT.JWTUtil;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetailsService;
import jy.Job_Flow_Agent.Redis.RedisService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWTUtil 단위 테스트")
class JWTUtilTest {

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private RedisService redisService;

    private static Resource privateKeyResource;
    private static Resource publicKeyResource;

    @BeforeAll
    static void generateRsaKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        String privateKeyPEM = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'})
                        .encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        String publicKeyPEM = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'})
                        .encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        privateKeyResource = new ByteArrayResource(privateKeyPEM.getBytes());
        publicKeyResource = new ByteArrayResource(publicKeyPEM.getBytes());
    }

    private JWTUtil createJWTUtil() throws Exception {
        return new JWTUtil(customUserDetailsService, redisService, privateKeyResource, publicKeyResource);
    }

    private Authentication mockAuth(String username) {
        Member member = Member.builder()
                .id(1L)
                .username(username)
                .displayName("테스트유저")
                .password("encoded-pw")
                .email(username + "@test.com")
                .roles(Set.of("ROLE_USER"))
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(member);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    // ─────────────────────────────────────────────────
    //  JU-01: Access Token 생성
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-01: Access Token 생성 - 비어있지 않은 JWT 문자열 반환")
    void createAccessToken_returnsNonBlankToken() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        Authentication auth = mockAuth("testuser");

        String token = jwtUtil.createAccessToken(auth);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    // ─────────────────────────────────────────────────
    //  JU-02: Access Token 클레임 검증
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-02: Access Token 클레임 - subject=username, userInfo 클레임 포함")
    void createAccessToken_claimsContainUsernameAndUserInfo() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        Authentication auth = mockAuth("testuser");

        String token = jwtUtil.createAccessToken(auth);
        Claims claims = jwtUtil.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("userInfo", String.class)).contains("testuser");
    }

    // ─────────────────────────────────────────────────
    //  JU-03: Refresh Token 생성
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-03: Refresh Token 생성 - 비어있지 않은 JWT 문자열 반환")
    void createRefreshToken_returnsNonBlankToken() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();

        String token = jwtUtil.createRefreshToken("testuser");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ─────────────────────────────────────────────────
    //  JU-04: 유효한 토큰 - 만료 아님
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-04: 방금 생성한 Access Token은 만료되지 않음")
    void isTokenExpired_freshToken_returnsFalse() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        Authentication auth = mockAuth("testuser");
        String token = jwtUtil.createAccessToken(auth);

        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  JU-05: 잘못된 형식 토큰 - 만료로 처리
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-05: 잘못된 형식의 토큰은 isTokenExpired = true 반환")
    void isTokenExpired_malformedToken_returnsTrue() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();

        assertThat(jwtUtil.isTokenExpired("not.a.valid.jwt")).isTrue();
    }

    // ─────────────────────────────────────────────────
    //  JU-06: 서명 위조 토큰 - 만료로 처리
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-06: 서명이 위조된 토큰은 isTokenExpired = true 반환")
    void isTokenExpired_tamperedSignature_returnsTrue() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        Authentication auth = mockAuth("testuser");
        String token = jwtUtil.createAccessToken(auth);

        // 마지막 signature 부분을 변조
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "INVALIDSIGNATURE";

        assertThat(jwtUtil.isTokenExpired(tamperedToken)).isTrue();
    }

    // ─────────────────────────────────────────────────
    //  JU-07: Access Token에서 username 추출
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-07: Access Token에서 username 정확히 추출")
    void extractUsername_fromAccessToken() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        Authentication auth = mockAuth("testuser");
        String token = jwtUtil.createAccessToken(auth);

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("testuser");
    }

    // ─────────────────────────────────────────────────
    //  JU-08: Refresh Token에서 username 추출
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-08: Refresh Token에서 username 정확히 추출")
    void extractUsername_fromRefreshToken() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        String token = jwtUtil.createRefreshToken("refreshuser");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("refreshuser");
    }

    // ─────────────────────────────────────────────────
    //  JU-09: Refresh Token Rotation 성공
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-09: Rotation 성공 - 새 토큰 반환 및 Redis setValue 호출 확인")
    void refreshTokenRotation_validToken_returnsNewToken() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        String oldToken = jwtUtil.createRefreshToken("testuser");

        given(redisService.getValue("refresh_token:testuser")).willReturn(oldToken);

        String newToken = jwtUtil.refreshTokenRotation(oldToken, "testuser");

        assertThat(newToken).isNotBlank();
        then(redisService).should()
                .setValue(eq("refresh_token:testuser"), anyString(), eq(604800L), eq(TimeUnit.SECONDS));
    }

    // ─────────────────────────────────────────────────
    //  JU-10: Rotation 실패 - Redis에 토큰 없음
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-10: Rotation 실패 - Redis에 저장된 토큰이 없으면 GlobalException 발생")
    void refreshTokenRotation_noStoredToken_throwsGlobalException() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();
        given(redisService.getValue("refresh_token:testuser")).willReturn(null);

        assertThatThrownBy(() -> jwtUtil.refreshTokenRotation("any-token", "testuser"))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("리프레시 토큰");
    }

    // ─────────────────────────────────────────────────
    //  JU-11: storeRefreshToken / removeRefreshToken
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JU-11: storeRefreshToken - Redis setValue 호출 확인")
    void storeRefreshToken_callsRedisSetValue() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();

        jwtUtil.storeRefreshToken("testuser", "my-refresh-token");

        then(redisService).should()
                .setValue("refresh_token:testuser", "my-refresh-token", 604800L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("JU-11: removeRefreshToken - Redis deleteValue 호출 확인")
    void removeRefreshToken_callsRedisDeleteValue() throws Exception {
        JWTUtil jwtUtil = createJWTUtil();

        jwtUtil.removeRefreshToken("testuser");

        then(redisService).should().deleteValue("refresh_token:testuser");
    }
}
