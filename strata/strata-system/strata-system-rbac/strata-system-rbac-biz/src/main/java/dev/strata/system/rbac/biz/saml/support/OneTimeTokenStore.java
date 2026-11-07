// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml.support;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Single-use token store for the SAML redirect handshake: RelayState -> AuthnRequest id, and one-time login code ->
 * refresh token. {@code memory} is fine for a single instance; {@code redis} uses GETDEL + TTL for multiple instances.
 */
public final class OneTimeTokenStore {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String label;
    private final boolean redis;
    private final StringRedisTemplate redisTemplate;
    private final String prefix;
    private final Duration ttl;
    private final Map<String, MemoryEntry> memory = new ConcurrentHashMap<>();

    public OneTimeTokenStore(
            String label, String store, StringRedisTemplate redisTemplate, String prefix, int ttlSeconds) {
        this.label = label;
        this.redis = "redis".equalsIgnoreCase(store);
        if (this.redis && redisTemplate == null) {
            throw new IllegalStateException(label + ": strata.saml.state-store=redis requires a StringRedisTemplate");
        }
        this.redisTemplate = redisTemplate;
        this.prefix = prefix;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public String newKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void put(String key, String value) {
        if (redis) {
            redisTemplate.opsForValue().set(prefix + key, value, ttl);
        } else {
            memory.put(key, new MemoryEntry(value, Instant.now().plus(ttl)));
        }
    }

    /** Single use: consumes and returns the value, or fails with UNAUTHORIZED. */
    public String take(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, label + " is missing");
        }
        String value = redis ? redisTemplate.opsForValue().getAndDelete(prefix + key) : takeMemory(key);
        if (value == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, label + " is invalid or already used");
        }
        return value;
    }

    private String takeMemory(String key) {
        MemoryEntry entry = memory.remove(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.value();
    }

    private record MemoryEntry(String value, Instant expiresAt) {}
}
