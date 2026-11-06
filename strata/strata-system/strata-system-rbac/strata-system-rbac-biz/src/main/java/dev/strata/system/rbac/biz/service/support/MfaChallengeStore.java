// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Short-lived, single-use MFA login challenges.
 *
 * <p>After a correct password for an MFA-enabled account, {@code /auth/login} does NOT issue a session: it
 * mints an opaque challenge tied to the user id. {@code /auth/mfa/verify} consumes it (at most once) after
 * the TOTP code checks out. This mirrors the in-memory {@link LoginLockService} store; like that component it
 * is single-JVM, which is acceptable while the deployment is one instance (see docs/08 §8 — single-JVM
 * buckets are known debt to move to Redis before horizontal scaling).
 */
@Component
public class MfaChallengeStore {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private record Entry(Long userId, Instant expiresAt) {}

    private final Map<String, Entry> challenges = new ConcurrentHashMap<>();

    /** Issues a fresh single-use challenge token bound to {@code userId}, valid for a few minutes. */
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        challenges.put(token, new Entry(userId, Instant.now().plus(CHALLENGE_TTL)));
        return token;
    }

    /**
     * Consumes a challenge: returns the bound user id and removes the token so it cannot be replayed. Throws
     * when the token is unknown, expired, or already used.
     */
    public Long consume(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.MFA_CHALLENGE_EXPIRED, "Missing MFA challenge token");
        }
        Entry entry = challenges.remove(token);
        if (entry == null) {
            throw new BusinessException(ErrorCode.MFA_CHALLENGE_EXPIRED, "MFA challenge is invalid or already used");
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.MFA_CHALLENGE_EXPIRED, "MFA challenge has expired");
        }
        return entry.userId();
    }

    /** Visible for tests / metrics: number of live (non-expired) challenges. */
    public int size() {
        Instant now = Instant.now();
        challenges.values().removeIf(entry -> entry.expiresAt().isBefore(now));
        return challenges.size();
    }
}
