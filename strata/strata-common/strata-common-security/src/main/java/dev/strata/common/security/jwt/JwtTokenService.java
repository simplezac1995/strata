// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signs and verifies staff tokens, and can do it across a key rotation.
 *
 * <p>Rotation is the reason this class knows about two keys. The usual way it fails is by forcing a mass logout:
 * rotate the secret and every issued token becomes invalid at once, so an operator either never rotates or does it
 * at 4am and accepts a wave of 401s. Here the new secret signs and the old one still verifies, so tokens drain
 * naturally as they expire and the old key can be dropped when the longest-lived one is gone.
 *
 * <p><b>Verification deliberately does not consult the {@code kid} header.</b> A header is attacker-controlled
 * input; letting it choose which key to try means letting the attacker choose which key's failure to hide behind.
 * The signature itself decides, and it can only be produced by a key we hold — so this tries each configured key in
 * turn and accepts exactly one outcome. The {@code kid} is still written, because it is what lets an operator see
 * which key signed a token in the logs, and it is never trusted for anything else.
 */
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    /** Where the fingerprint goes in the token header, for operator visibility only. */
    private static final String KEY_ID_HEADER = "kid";

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final List<SecretKey> verificationKeys;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = keyOf(properties.secret());
        List<SecretKey> candidates = new ArrayList<>(2);
        candidates.add(signingKey);
        if (properties.hasPreviousSecret()) {
            candidates.add(keyOf(properties.previousSecret()));
            log.info(
                    "JWT rotation is in progress: signing with key {} while key {} still verifies",
                    fingerprint(),
                    fingerprintOf(properties.previousSecret()));
        }
        this.verificationKeys = List.copyOf(candidates);
    }

    public String createAccessToken(Long userId, String username, Collection<String> authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header()
                .keyId(fingerprint())
                .and()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("authorities", List.copyOf(authorities))
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.accessTokenTtlSeconds())))
                .signWith(signingKey)
                .compact();
    }

    public String createRefreshToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header()
                .keyId(fingerprint())
                .and()
                .id(java.util.UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.refreshTokenTtlSeconds())))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies a token against every configured key.
     *
     * <p>A signature is produced by exactly one key, so trying two is not a weakening: each attempt is a full HMAC
     * verification, and a token that verifies under neither is rejected. The cost is one extra hash for tokens
     * issued before a rotation, which is a bounded, temporary price for not logging everyone out.
     *
     * @throws JwtException when no configured key verifies the token
     */
    public Claims parse(String token) {
        JwtException last = null;
        for (SecretKey candidate : verificationKeys) {
            try {
                return Jwts.parser()
                        .verifyWith(candidate)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (JwtException rejected) {
                log.error("Error in JwtTokenService: {}", rejected.getMessage(), rejected);
                last = rejected;
            }
        }
        throw last == null ? new JwtException("No JWT key is configured") : last;
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /**
     * A short, non-reversible label for the key currently signing.
     *
     * <p>It exists so a log line can answer "which key is this instance using, and is a rotation in progress?"
     * without ever putting key material in a log. Eight hex characters of a SHA-256 is enough to compare two
     * deployments by eye and useless for reproducing the secret.
     */
    public String fingerprint() {
        return fingerprintOf(properties.secret());
    }

    static String fingerprintOf(String secret) {
        try {
            byte[] digest =
                    java.security.MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(8);
            for (int index = 0; index < 4; index++) {
                hex.append(Character.forDigit((digest[index] >> 4) & 0xF, 16))
                        .append(Character.forDigit(digest[index] & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            log.error("Error in JwtTokenService: {}", impossible.getMessage(), impossible);
            // SHA-256 is required of every JVM; a failure here is not a configuration problem.
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static SecretKey keyOf(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    static String keyIdHeader() {
        return KEY_ID_HEADER;
    }
}
