package me.aloic.apeurival.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class TokenInvalidationStore {

    private static final String KEY_PREFIX = "user:token:minIat:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final boolean available;

    public TokenInvalidationStore(StringRedisTemplate redis) {
        this.redis = redis;
        this.available = checkConnection(redis.getConnectionFactory());
    }

    private boolean checkConnection(RedisConnectionFactory factory) {
        try {
            String ping = factory.getConnection().ping();
            log.info("Redis connected: PING -> {}", ping);
            return true;
        } catch (Exception e) {
            log.warn("Redis unavailable — token invalidation disabled. Message: {}", e.getMessage());
            return false;
        }
    }

    public void invalidateAllTokens(Long userId) {
        if (!available) {
            log.debug("Redis unavailable, skipping token invalidation for user {}", userId);
            return;
        }
        try {
            long now = System.currentTimeMillis() / 1000;
            String key = KEY_PREFIX + userId;
            redis.opsForValue().set(key, String.valueOf(now), TTL);
            log.info("Token invalidation: minIat={} for user {}", now, userId);
        } catch (Exception e) {
            log.warn("Failed to invalidate tokens for user {}: {}", userId, e.getMessage());
        }
    }

    public boolean isTokenValid(Long userId, long tokenIat) {
        if (!available) {
            return true;
        }
        try {
            String val = redis.opsForValue().get(KEY_PREFIX + userId);
            if (val == null) {
                return true;
            }
            long minIat = Long.parseLong(val);
            boolean valid = tokenIat >= minIat;
            if (!valid) {
                log.info("Token rejected: user {} iat={} < minIat={}", userId, tokenIat, minIat);
            }
            return valid;
        } catch (Exception e) {
            log.warn("Redis error during token check for user {}: {}", userId, e.getMessage());
            return true;
        }
    }
}
