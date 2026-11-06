// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Server-side, single-use {@code state} nonce + PKCE {@code code_verifier} store for social login (P2-9).
 *
 * <p>Modeled on {@link MfaChallengeStore}: the authorize endpoint mints an opaque {@code state} and a PKCE
 * {@code code_verifier}, keeps both server-side (the verifier must never round-trip through the browser), and
 * the callback consumes the pair exactly once. Unknown / expired / replayed states are rejected. Like the
 * other challenge stores this is single-JVM today — same known-debt note as {@link MfaChallengeStore} (move
 * to Redis before horizontal scaling, see docs/08 §8).
 */
@Component
public class SocialLoginStateStore {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** What {@link #issue} hands back: the public state to echo, plus the derived S256 challenge. */
    public record Session(
            String state, String codeVerifier, String codeChallenge, String provider, String redirectUri) {}

    /** What {@link #consume} returns: the server-side material bound to a valid, one-time state. */
    public record Entry(
            String codeVerifier, String codeChallenge, String provider, String redirectUri, Instant expiresAt) {}

    private final Map<String, Entry> states = new ConcurrentHashMap<>();

    /**
     * Mints a fresh single-use state bound to the provider and requested redirect_uri, generates a PKCE
     * code_verifier and its S256 challenge, and persists the pair server-side.
     */
    public Session issue(String provider, String redirectUri) {
        String state = randomToken(32);
        String codeVerifier = randomToken(32);
        String codeChallenge = s256Challenge(codeVerifier);
        states.put(
                state,
                new Entry(
                        codeVerifier,
                        codeChallenge,
                        provider,
                        redirectUri,
                        Instant.now().plus(STATE_TTL)));
        return new Session(state, codeVerifier, codeChallenge, provider, redirectUri);
    }

    /**
     * Consumes a state: removes it so it cannot be replayed and returns the bound material. Throws when the
     * state is missing, unknown, expired, or already used.
     */
    public Entry consume(String state) {
        if (state == null || state.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing social login state");
        }
        Entry entry = states.remove(state);
        if (entry == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Social login state is invalid or already used");
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Social login state has expired");
        }
        return entry;
    }

    private static String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        SECURE_RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** RFC 7636 S256: BASE64URL-NO-PAD(SHA256(verifier)). */
    private static String s256Challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
