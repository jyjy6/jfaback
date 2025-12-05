package jy.Job_Flow_Agent.Redis.RateLimit;




import jy.Job_Flow_Agent.Redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/rate-limit")
@RequiredArgsConstructor
@Slf4j
public class RateLimitController {

    private final RedisService redisService;


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
            @RequestParam String key,
            @RequestParam long windowSeconds,
            @RequestParam(defaultValue = "FIXED_WINDOW") String type
    ) {
        Map<String, Object> status = new HashMap<>();

        try {
            RateLimit.RateLimitType rateLimitType = RateLimit.RateLimitType.valueOf(type.toUpperCase());


            long currentCount = redisService.getCurrentRequestCount(key, windowSeconds, rateLimitType);
            log.info("key: {}, currentCount: {}", key, currentCount);

            status.put("key", key);
            status.put("windowSeconds", windowSeconds);
            status.put("currentCount", currentCount);
            status.put("status", "active");

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Rate limit 상태 조회 실패: {}", e.getMessage());
            status.put("error", e.getMessage());
            status.put("status", "error");
            return ResponseEntity.badRequest().body(status);
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetRateLimit(@RequestParam String key) {
        Map<String, Object> result = new HashMap<>();

        try {
            redisService.deleteSpecificKey(key);
            result.put("message", "Rate limit 초기화 완료");
            result.put("key", key);
            result.put("status", "success");

            log.info("Rate limit 초기화 완료: {}", key);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Rate limit 초기화 실패: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("status", "error");
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> getAllRateLimitKeys() {
        Map<String, Object> result = new HashMap<>();

        try {
            Set<String> keys = redisService.getAllKeys();
            Set<String> rateLimitKeys = keys.stream()
                    .filter(key -> key.startsWith("rate_limit:"))
                    .collect(java.util.stream.Collectors.toSet());

            result.put("keys", rateLimitKeys);
            result.put("totalCount", rateLimitKeys.size());
            result.put("status", "success");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Rate limit 키 조회 실패: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("status", "error");
            return ResponseEntity.badRequest().body(result);
        }
    }

    @RateLimit(
            windowSeconds = 10,
            maxRequests = 3,
            identifierType = RateLimit.IdentifierType.IP,
            type = RateLimit.RateLimitType.SLIDING_WINDOW,
            message = "테스트 Rate Limit: 10초간 3회 제한"
    )
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testRateLimit() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Rate Limit 테스트 성공");
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "success");

        return ResponseEntity.ok(result);
    }

    @RateLimit(
            type = RateLimit.RateLimitType.TOKEN_BUCKET,
            capacity = 5,
            refillRate = 0.5,
            identifierType = RateLimit.IdentifierType.IP,
            message = "Token Bucket 테스트: 최대 5개 토큰, 초당 0.5개 리필"
    )
    @GetMapping("/test/token-bucket")
    public ResponseEntity<Map<String, Object>> testTokenBucket() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Token Bucket 테스트 성공");
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "success");

        return ResponseEntity.ok(result);
    }
}