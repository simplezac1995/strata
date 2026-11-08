// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import dev.strata.system.rbac.biz.config.TokenBlacklistProperties;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Server-side revocation for refresh tokens (ADR-0008). Redis-backed when enabled; a no-op
 * otherwise so local single-process development can run without Redis infrastructure.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private static final String KEY_PREFIX = "strata:refresh:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;

    public TokenBlacklistService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider, TokenBlacklistProperties properties) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.enabled = properties.blacklistEnabled() && this.redisTemplate != null;
    }

    /** Blacklists the token's jti for its remaining lifetime. Expired tokens are ignored. */
    public void blacklist(Claims claims) {
        if (!enabled || claims.getId() == null) {
            return;
        }
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return;
        }
        long ttlSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + claims.getId(), "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(Claims claims) {
        if (!enabled || claims.getId() == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + claims.getId()));
    }

    public boolean isEnabled() {
        return enabled;
    }
}
