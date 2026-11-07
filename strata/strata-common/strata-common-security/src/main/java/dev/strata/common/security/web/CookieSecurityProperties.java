// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Browser cookie hardening for the admin surface (P1-6).
 *
 * <p>The app currently authenticates with a JWT carried in JSON, but any cookie the server does emit (session,
 * refresh, third-party integration) must not be usable from a non-browser or cross-site context. When {@code
 * hardened} is on, the response wrapper forces every {@code Set-Cookie} to carry the attributes below and the
 * {@code __Host-} prefix.
 *
 * @param hardened when true, rewrite every Set-Cookie to enforce the secure attributes. Production turns this on.
 * @param sameSite the SameSite attribute to force (Lax by default).
 * @param hostPrefix when true, rename the cookie to the {@code __Host-} prefix.
 * @param maxConcurrentSessions maximum concurrent authenticated sessions per user (0 = unlimited). The login flow
 *     enforces this; exposed here so the value is one documented knob.
 * @param idleTimeout sessions idle longer than this are evicted (0 = never idle-expire).
 */
@ConfigurationProperties(prefix = "strata.security.cookie")
public record CookieSecurityProperties(
        @DefaultValue("false") boolean hardened,
        @DefaultValue("Lax") String sameSite,
        @DefaultValue("true") boolean hostPrefix,
        @DefaultValue("5") int maxConcurrentSessions,
        @DefaultValue("PT30M") Duration idleTimeout) {

    public CookieSecurityProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
        if (maxConcurrentSessions < 0) {
            maxConcurrentSessions = 0;
        }
        if (idleTimeout == null || idleTimeout.isNegative()) {
            idleTimeout = Duration.ZERO;
        }
    }
}
