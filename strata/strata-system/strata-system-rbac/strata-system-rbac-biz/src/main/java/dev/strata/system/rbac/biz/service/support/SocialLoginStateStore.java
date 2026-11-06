// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * Server-side, single-use {@code state} nonce + PKCE {@code code_verifier} store for social login.
 *
 * <p>The authorize endpoint mints an opaque {@code state} and a PKCE {@code code_verifier}, keeps both server-side
 * (the verifier must never round-trip through the browser), and the callback consumes the pair exactly once.
 * Unknown / expired / replayed states are rejected.
 *
 * <p>Backends: {@code memory} (default, single JVM) and {@code redis} (multi-instance deployments, atomic GETDEL and a
 * TTL). Select with {@code strata.social.state-store=redis}.
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

    private final StateBackend backend;

    /** Single-JVM default used by unit tests and the {@code memory} backend. */
    public SocialLoginStateStore() {
        this.backend = new MemoryStateBackend();
    }

    @Autowired
    public SocialLoginStateStore(SocialLoginProperties properties, ObjectProvider<StringRedisTemplate> redisTemplate) {
        this(properties, redisTemplate.getIfAvailable());
    }

    /** Test-friendly constructor: forces the redis backend when a template is supplied. */
    SocialLoginStateStore(SocialLoginProperties properties, StringRedisTemplate redisTemplate) {
        if (!"memory".equalsIgnoreCase(properties.getStateStore())) {
            if (!"redis".equalsIgnoreCase(properties.getStateStore())) {
                throw new IllegalArgumentException(
                        "Unsupported strata.social.state-store: " + properties.getStateStore());
            }
            if (redisTemplate == null) {
                throw new IllegalStateException("strata.social.state-store=redis requires a StringRedisTemplate bean");
            }
            this.backend = new RedisStateBackend(redisTemplate);
        } else {
            this.backend = new MemoryStateBackend();
        }
    }

    /**
     * Mints a fresh state / PKCE pair, stores the server-side material and returns what the browser may see.
     */
    public Session issue(String provider, String redirectUri) {
        String state = randomToken(32);
        String codeVerifier = randomToken(32);
        String codeChallenge = s256Challenge(codeVerifier);
        Entry entry = new Entry(
                codeVerifier,
                codeChallenge,
                provider,
                redirectUri,
                Instant.now().plus(STATE_TTL));
        backend.put(state, entry);
        return new Session(state, codeVerifier, codeChallenge, provider, redirectUri);
    }

    /**
     * Consumes a state exactly once. Rejects blank / unknown / expired / replayed states.
     */
    public Entry consume(String state) {
        if (state == null || state.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing social login state");
        }
        Entry entry = backend.take(state);
        if (entry == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Social login state is invalid or already used");
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Social login state has expired");
        }
        return entry;
    }

    /** Storage strategy; both implementations are single-use. */
    private interface StateBackend {
        void put(String state, Entry entry);

        Entry take(String state);
    }

    private static final class MemoryStateBackend implements StateBackend {
        private final Map<String, Entry> states = new ConcurrentHashMap<>();

        @Override
        public void put(String state, Entry entry) {
            states.put(state, entry);
        }

        @Override
        public Entry take(String state) {
            return states.remove(state);
        }
    }

    private static final class RedisStateBackend implements StateBackend {
        private static final String KEY_PREFIX = "strata:social:state:";
        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final ValueOperations<String, String> values;

        private RedisStateBackend(StringRedisTemplate redisTemplate) {
            this.values = redisTemplate.opsForValue();
        }

        @Override
        public void put(String state, Entry entry) {
            try {
                String payload = MAPPER.writeValueAsString(Map.of(
                        "cv", entry.codeVerifier(),
                        "cc", entry.codeChallenge(),
                        "p", entry.provider(),
                        "ru", entry.redirectUri(),
                        "exp", entry.expiresAt().toEpochMilli()));
                values.set(KEY_PREFIX + state, payload, STATE_TTL);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not persist the social login state", exception);
            }
        }

        @Override
        public Entry take(String state) {
            String payload = values.getAndDelete(KEY_PREFIX + state);
            if (payload == null) {
                return null;
            }
            try {
                JsonNode node = MAPPER.readTree(payload);
                return new Entry(
                        node.path("cv").asText(),
                        node.path("cc").asText(),
                        node.path("p").asText(),
                        node.path("ru").asText(),
                        Instant.ofEpochMilli(node.path("exp").asLong()));
            } catch (Exception exception) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Social login state is invalid");
            }
        }
    }

    private static String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        SECURE_RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
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
