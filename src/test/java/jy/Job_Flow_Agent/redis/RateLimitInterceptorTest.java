package jy.Job_Flow_Agent.redis;

import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Redis.RateLimit.RateLimit;
import jy.Job_Flow_Agent.Redis.RateLimit.RateLimitInterceptor;
import jy.Job_Flow_Agent.Redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor 단위 테스트")
class RateLimitInterceptorTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private RateLimitInterceptor interceptor;

    // 테스트용 컨트롤러 - @RateLimit이 붙은 메서드를 제공하기 위해 사용
    static class TestController {

        @RateLimit(windowSeconds = 60, maxRequests = 3,
                type = RateLimit.RateLimitType.FIXED_WINDOW,
                identifierType = RateLimit.IdentifierType.IP)
        public void rateLimitedMethod() {}

        @RateLimit(windowSeconds = 60, maxRequests = 5,
                type = RateLimit.RateLimitType.FIXED_WINDOW,
                identifierType = RateLimit.IdentifierType.USER_ID)
        public void userIdRateLimitedMethod() {}

        public void noRateLimitMethod() {}
    }

    private HandlerMethod handlerMethodFor(String methodName) throws NoSuchMethodException {
        TestController controller = new TestController();
        Method method = TestController.class.getMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────
    //  RL-01: Rate Limit 범위 내 요청 → true 반환, 헤더 포함
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RL-01: Rate Limit 범위 내 요청 → preHandle true, 잔여량 헤더 설정")
    void preHandle_withinLimit_returnsTrueAndSetsHeaders() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethodFor("rateLimitedMethod");

        given(redisService.isAllowedFixedWindow(anyString(), anyLong(), anyInt())).willReturn(true);
        given(redisService.getCurrentRequestCount(anyString(), anyLong(), any())).willReturn(1L);

        // when
        boolean result = interceptor.preHandle(request, response, handler);

        // then
        assertThat(result).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("3");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
    }

    // ─────────────────────────────────────────────────
    //  RL-02: Rate Limit 초과 → GlobalException 발생
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RL-02: Rate Limit 초과 → GlobalException(RATE_LIMIT_EXCEEDED) 발생")
    void preHandle_exceedLimit_throwsGlobalException() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethodFor("rateLimitedMethod");

        given(redisService.isAllowedFixedWindow(anyString(), anyLong(), anyInt())).willReturn(false);
        given(redisService.getCurrentRequestCount(anyString(), anyLong(), any())).willReturn(4L);

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED"));
    }

    // ─────────────────────────────────────────────────
    //  RL-03: IP 기반 식별 → IP 키 사용 확인
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RL-03: identifierType=IP → IP 기반 Redis 키 사용 확인")
    void preHandle_ipIdentifier_usesIpBasedKey() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethodFor("rateLimitedMethod");

        given(redisService.isAllowedFixedWindow(argThat(k -> k.contains("ip:10.0.0.1")), anyLong(), anyInt()))
                .willReturn(true);
        given(redisService.getCurrentRequestCount(anyString(), anyLong(), any())).willReturn(1L);

        // when
        boolean result = interceptor.preHandle(request, response, handler);

        // then
        assertThat(result).isTrue();
    }

    // ─────────────────────────────────────────────────
    //  RL-04: USER_ID 기반 식별 → 사용자 키 사용 확인
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RL-04: identifierType=USER_ID, 로그인 상태 → user: 키 포함 확인")
    void preHandle_userIdIdentifier_usesUserBasedKey() throws Exception {
        // given - SecurityContext에 인증 정보 설정
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "testuser", null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethodFor("userIdRateLimitedMethod");

        given(redisService.isAllowedFixedWindow(argThat(k -> k.contains("user:testuser")), anyLong(), anyInt()))
                .willReturn(true);
        given(redisService.getCurrentRequestCount(anyString(), anyLong(), any())).willReturn(1L);

        // when
        boolean result = interceptor.preHandle(request, response, handler);

        // then
        assertThat(result).isTrue();
    }

    // ─────────────────────────────────────────────────
    //  RL-05: @RateLimit 없는 핸들러 → Rate Limit 체크 없이 통과
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RL-05: @RateLimit 없는 메서드 → Rate Limit 체크 없이 true 반환")
    void preHandle_noRateLimitAnnotation_returnsTrueWithoutCheck() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethodFor("noRateLimitMethod");

        // when
        boolean result = interceptor.preHandle(request, response, handler);

        // then
        assertThat(result).isTrue();
        // RedisService 호출 없음 확인
        org.mockito.Mockito.verifyNoInteractions(redisService);
    }
}
