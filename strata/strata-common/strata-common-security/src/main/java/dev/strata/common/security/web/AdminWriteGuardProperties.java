// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Server-side guard for state-changing admin requests (P1-5).
 *
 * <p>A stateless JSON API cannot rely on a CSRF cookie token; the two cheap signals it does have are enforced here on
 * the staff surface ({@code /api/**} writes): the browser's {@code Origin} must be in the allow-list, and the write
 * must be JSON.
 *
 * @param enabled master switch for both checks.
 * @param allowedOrigins exact or {@code *}-pattern origins a state-changing request may come from (e.g.
 *     {@code https://admin.example.com}, {@code http://localhost:*}). When <b>empty</b> the Origin check is skipped
 *     (local development); production must set it non-empty.
 */
@ConfigurationProperties(prefix = "strata.security.admin-write-guard")
public record AdminWriteGuardProperties(
        @DefaultValue("true") boolean enabled, @DefaultValue({}) List<String> allowedOrigins) {

    public AdminWriteGuardProperties {
        if (allowedOrigins == null) {
            allowedOrigins = new ArrayList<>();
        }
    }

    /** When false, the allow-list check is not enforced (dev); production must configure at least one origin. */
    public boolean originCheckEnabled() {
        return enabled && !allowedOrigins.isEmpty();
    }
}
