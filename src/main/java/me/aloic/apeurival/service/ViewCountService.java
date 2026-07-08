package me.aloic.apeurival.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ViewCountService {

    private static final String PREFIX_POST = "view:post:";
    private static final String PREFIX_WORK = "view:work:";

    private final StringRedisTemplate redis;
    private final boolean available;

    public ViewCountService(StringRedisTemplate redis) {
        this.redis = redis;
        this.available = checkConnection();
    }

    private boolean checkConnection() {
        try {
            redis.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis unavailable — view counting disabled");
            return false;
        }
    }

    public void incrementPost(Long postId) {
        if (!available) return;
        try {
            redis.opsForValue().increment(PREFIX_POST + postId);
        } catch (Exception e) {
            log.debug("Redis increment failed for post {}: {}", postId, e.getMessage());
        }
    }

    public void incrementWork(Long workId) {
        if (!available) return;
        try {
            redis.opsForValue().increment(PREFIX_WORK + workId);
        } catch (Exception e) {
            log.debug("Redis increment failed for work {}: {}", workId, e.getMessage());
        }
    }

    public long getAndResetPost(Long postId) {
        try {
            String key = PREFIX_POST + postId;
            String val = redis.opsForValue().getAndDelete(key);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getAndResetWork(Long workId) {
        try {
            String key = PREFIX_WORK + workId;
            String val = redis.opsForValue().getAndDelete(key);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public long mergePost(Long postId, long dbCount) {
        if (!available) return dbCount;
        try {
            String val = redis.opsForValue().get(PREFIX_POST + postId);
            return val != null ? dbCount + Long.parseLong(val) : dbCount;
        } catch (Exception e) {
            return dbCount;
        }
    }

    public long mergeWork(Long workId, long dbCount) {
        if (!available) return dbCount;
        try {
            String val = redis.opsForValue().get(PREFIX_WORK + workId);
            return val != null ? dbCount + Long.parseLong(val) : dbCount;
        } catch (Exception e) {
            return dbCount;
        }
    }
}
