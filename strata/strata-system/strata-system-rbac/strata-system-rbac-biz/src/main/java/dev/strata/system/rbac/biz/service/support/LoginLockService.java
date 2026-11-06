// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.rbac.biz.config.LoginLockProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Login failure lockout. Store is switchable: {@code memory} (default, works without infra) or
 * {@code redis} (shared across instances — set via {@code strata.security.login-lock.store}).
 */
@Component
public class LoginLockService {

    private static final Logger log = LoggerFactory.getLogger(LoginLockService.class);

    private static final String REDIS_KEY_PREFIX = "strata:login-lock:";

    private record Entry(int failures, Instant lockedUntil) {}

    private final Map<String, Entry> memoryEntries = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final boolean useRedis;
    private final LoginLockProperties properties;

    public LoginLockService(LoginLockProperties properties, ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.useRedis = "redis".equalsIgnoreCase(properties.store()) && this.redisTemplate != null;
    }

    public void checkNotLocked(String key) {
        int failures = useRedis ? redisFailures(key) : memoryFailures(key);
        if (failures >= properties.maxAttempts()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "Too many failed attempts, try again later");
        }
    }

    public void recordFailure(String key) {
        if (useRedis) {
            redisTemplate.opsForValue().increment(REDIS_KEY_PREFIX + key);
            redisTemplate.expire(REDIS_KEY_PREFIX + key, Duration.ofMinutes(properties.lockMinutes()));
        } else {
            Entry updated = memoryEntries.compute(key, (ignored, current) -> {
                if (current == null || current.lockedUntil() != null) {
                    return new Entry(1, null);
                }
                return new Entry(current.failures() + 1, null);
            });
            if (updated.failures() >= properties.maxAttempts()) {
                Instant lockedUntil = Instant.now().plus(Duration.ofMinutes(properties.lockMinutes()));
                memoryEntries.put(key, new Entry(updated.failures(), lockedUntil));
            }
        }
    }

    public void clear(String key) {
        if (useRedis) {
            redisTemplate.delete(REDIS_KEY_PREFIX + key);
        } else {
            memoryEntries.remove(key);
        }
    }

    private int redisFailures(String key) {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            log.error("Error in LoginLockService: {}", ex.getMessage(), ex);
            return 0;
        }
    }

    private int memoryFailures(String key) {
        Entry entry = memoryEntries.get(key);
        if (entry == null) {
            return 0;
        }
        if (entry.lockedUntil() != null && !entry.lockedUntil().isAfter(Instant.now())) {
            memoryEntries.remove(key);
            return 0;
        }
        return entry.failures();
    }
}
