// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Signing material for staff tokens.
 *
 * <p>{@code previousSecret} is what makes rotation possible without logging everyone out: an operator sets the new
 * secret as {@code secret}, moves the old one here, and every token that was already issued keeps verifying until
 * it expires. Tokens are signed with {@code secret} only, so the overlap is one-directional and finite — once
 * {@code previousSecret} is cleared, the old key is gone.
 *
 * <p>Both are secrets in the literal sense: they must never be logged, and the service logs a fingerprint instead
 * (see {@link JwtTokenService#fingerprint()}).
 */
@ConfigurationProperties(prefix = "strata.security.jwt")
public record JwtProperties(
        String secret, String previousSecret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "strata.security.jwt.secret is required (set STRATA_JWT_SECRET or application.yml default for local)");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("strata.security.jwt.secret must be at least 32 characters");
        }
        // A previous secret that is too short is rejected rather than ignored: silently dropping it would log out
        // every user at the exact moment an operator believed they had arranged a graceful rotation.
        if (previousSecret != null && !previousSecret.isBlank() && previousSecret.length() < 32) {
            throw new IllegalArgumentException("strata.security.jwt.previous-secret must be at least 32 characters");
        }
        if (accessTokenTtlSeconds <= 0) {
            accessTokenTtlSeconds = 7200;
        }
        if (refreshTokenTtlSeconds <= 0) {
            refreshTokenTtlSeconds = 604800;
        }
    }

    public boolean hasPreviousSecret() {
        return previousSecret != null && !previousSecret.isBlank();
    }
}
