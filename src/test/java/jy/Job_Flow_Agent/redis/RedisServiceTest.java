package jy.Job_Flow_Agent.redis;

import jy.Job_Flow_Agent.Redis.RateLimit.RateLimit;
import jy.Job_Flow_Agent.Redis.RedisService;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("RedisService 단위 테스트 (TestContainers Redis)")
class RedisServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RedisTemplate<String, Object> redisTemplate;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379))
        );
        factory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        redisService = new RedisService(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 Redis 데이터 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    // ─────────────────────────────────────────────────
    //  RDS-01: 기본 set / get / delete
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RDS-01: set → get → delete 기본 동작")
    void setValue_getValue_deleteValue() {
        redisService.setValue("test:key", "hello");

        assertThat(redisService.getValue("test:key")).isEqualTo("hello");

        redisService.deleteValue("test:key");

        assertThat(redisService.getValue("test:key")).isNull();
    }

    // ─────────────────────────────────────────────────
    //  RDS-02: TTL 설정 → 만료 후 null
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RDS-02: TTL=1초 설정 후 2초 대기 → null 반환")
    void setValue_withTtl_expiresAfterTtl() throws InterruptedException {
        redisService.setValue("ttl:key", "expires", 1, TimeUnit.SECONDS);

        assertThat(redisService.getValue("ttl:key")).isEqualTo("expires");

        Thread.sleep(1500);

        assertThat(redisService.getValue("ttl:key")).isNull();
    }

    // ─────────────────────────────────────────────────
    //  RDS-03 / RDS-04: Fixed Window Rate Limit
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RDS-03: Fixed Window - maxRequests=3 내 요청 → true 반환")
    void fixedWindow_withinLimit_returnsTrue() {
        boolean first = redisService.isAllowedFixedWindow("fw:user1", 60, 3);
        boolean second = redisService.isAllowedFixedWindow("fw:user1", 60, 3);
        boolean third = redisService.isAllowedFixedWindow("fw:user1", 60, 3);

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(third).isTrue();
    }

    @Test
    @DisplayName("RDS-04: Fixed Window - maxRequests=3 초과 요청 → false 반환")
    void fixedWindow_exceedLimit_returnsFalse() {
        redisService.isAllowedFixedWindow("fw:user2", 60, 3);
        redisService.isAllowedFixedWindow("fw:user2", 60, 3);
        redisService.isAllowedFixedWindow("fw:user2", 60, 3);

        boolean fourth = redisService.isAllowedFixedWindow("fw:user2", 60, 3);

        assertThat(fourth).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  RDS-05 / RDS-06: Sliding Window Rate Limit
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RDS-05: Sliding Window - maxRequests=3 내 요청 → true 반환")
    void slidingWindow_withinLimit_returnsTrue() {
        boolean first = redisService.isAllowedSlidingWindow("sw:user1", 60, 3);
        boolean second = redisService.isAllowedSlidingWindow("sw:user1", 60, 3);
        boolean third = redisService.isAllowedSlidingWindow("sw:user1", 60, 3);

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(third).isTrue();
    }

    @Test
    @DisplayName("RDS-06: Sliding Window - maxRequests=3 초과 → false 반환")
    void slidingWindow_exceedLimit_returnsFalse() {
        redisService.isAllowedSlidingWindow("sw:user2", 60, 3);
        redisService.isAllowedSlidingWindow("sw:user2", 60, 3);
        redisService.isAllowedSlidingWindow("sw:user2", 60, 3);

        boolean fourth = redisService.isAllowedSlidingWindow("sw:user2", 60, 3);

        assertThat(fourth).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  RDS-07 / RDS-08: Token Bucket Rate Limit
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RDS-07: Token Bucket - capacity=3, 3회 요청 → 모두 true")
    void tokenBucket_withinCapacity_returnsTrue() {
        boolean first = redisService.isAllowedTokenBucket("tb:user1", 3, 0.0);
        boolean second = redisService.isAllowedTokenBucket("tb:user1", 3, 0.0);
        boolean third = redisService.isAllowedTokenBucket("tb:user1", 3, 0.0);

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(third).isTrue();
    }

    @Test
    @DisplayName("RDS-08: Token Bucket - capacity=2 소진 후 요청 → false 반환")
    void tokenBucket_exhausted_returnsFalse() {
        redisService.isAllowedTokenBucket("tb:user2", 2, 0.0);
        redisService.isAllowedTokenBucket("tb:user2", 2, 0.0);

        boolean third = redisService.isAllowedTokenBucket("tb:user2", 2, 0.0);

        assertThat(third).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  RDS-09 / RDS-10: 분산 락
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RDS-09: acquireLock() 최초 시도 → true, 키 존재 확인")
    void acquireLock_firstAttempt_returnsTrue() {
        boolean acquired = redisService.acquireLock("lock:resource1", "owner1", 10, TimeUnit.SECONDS);

        assertThat(acquired).isTrue();
        assertThat(redisService.hasKey("lock:resource1")).isTrue();
    }

    @Test
    @DisplayName("RDS-10: acquireLock() 동일 락 2회 시도 → 두 번째 false 반환")
    void acquireLock_duplicate_returnsFalse() {
        redisService.acquireLock("lock:resource2", "owner1", 10, TimeUnit.SECONDS);

        boolean second = redisService.acquireLock("lock:resource2", "owner2", 10, TimeUnit.SECONDS);

        assertThat(second).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  RDS-11: Refresh Token 저장/조회/삭제 시뮬레이션
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RDS-11: Refresh Token 저장 → 조회 일치 → 삭제 후 null")
    void refreshTokenLifecycle() {
        String key = "refresh_token:testuser";
        String token = "my.refresh.token";

        redisService.setValue(key, token, 604800, TimeUnit.SECONDS);

        assertThat(redisService.getValue(key)).isEqualTo(token);

        redisService.deleteValue(key);

        assertThat(redisService.getValue(key)).isNull();
    }
}
