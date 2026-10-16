package com.pulsewatch.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // In-memory fallback
    private final ConcurrentHashMap<String, AttemptTracker> localCache = new ConcurrentHashMap<>();

    private static class AttemptTracker {
        final AtomicInteger count = new AtomicInteger(0);
        final long expiresAt;

        AttemptTracker(long ttlSeconds) {
            this.expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public boolean isAllowed(String key, int maxAttempts, long windowSeconds) {
        if (redisTemplate != null) {
            try {
                String redisKey = "ratelimit:" + key;
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
                }
                return count != null && count <= maxAttempts;
            } catch (Exception e) {
                log.warn("Redis unavailable for rate limiting, falling back to local memory: {}", e.getMessage());
            }
        }

        // Local In-Memory Fallback
        AttemptTracker tracker = localCache.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                AttemptTracker newTracker = new AttemptTracker(windowSeconds);
                newTracker.count.incrementAndGet();
                return newTracker;
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return tracker.count.get() <= maxAttempts;
    }

    public void reset(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("ratelimit:" + key);
            } catch (Exception ignored) {}
        }
        localCache.remove(key);
    }
}
