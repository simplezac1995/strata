// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Idempotency settings. The filter is inert unless the caller sends the header, so a missing
 * {@code Idempotency-Key} is only an error where {@code required} (or {@code requiredPathPatterns}) says so.
 *
 * <p>{@code methods} deliberately excludes GET/HEAD: reads are already idempotent and replaying a
 * cached body would freeze stale data.
 *
 * <p>{@code pathPatterns} covers both the staff prefix ({@code /api/v1/**}) and the player prefix
 * ({@code /v1/**}, ADR-0026), so the same filter protects both surfaces.
 *
 * <p>{@code required} forces the header everywhere, which is only right when every write on the surface is
 * money-shaped. On a mixed surface (admin CRUD next to payments) use {@code requiredPathPatterns}: those
 * paths must carry a key, and they participate even when {@code pathPatterns} does not list them.
 */
@ConfigurationProperties(prefix = "strata.idempotency")
public record IdempotencyProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean required,
        @DefaultValue("Idempotency-Key") String headerName,
        @DefaultValue({"/api/v1/**", "/v1/**"}) List<String> pathPatterns,
        @DefaultValue({}) List<String> requiredPathPatterns,
        @DefaultValue({"POST", "PUT", "PATCH", "DELETE"}) List<String> methods,
        @DefaultValue("PT24H") Duration retention,
        @DefaultValue("65536") int maxBodyBytes,
        @DefaultValue("true") boolean purgeEnabled,
        @DefaultValue("PT30M") Duration purgeInterval) {

    /** Keys longer than the column would only ever fail on insert, so reject them while they are cheap. */
    public static final int MAX_KEY_LENGTH = 128;

    public IdempotencyProperties {
        if (headerName == null || headerName.isBlank()) {
            headerName = "Idempotency-Key";
        }
        if (pathPatterns == null || pathPatterns.isEmpty()) {
            pathPatterns = List.of("/api/v1/**", "/v1/**");
        }
        if (requiredPathPatterns == null) {
            requiredPathPatterns = List.of();
        }
        if (methods == null || methods.isEmpty()) {
            methods = List.of("POST", "PUT", "PATCH", "DELETE");
        }
        if (retention == null || retention.isZero() || retention.isNegative()) {
            retention = Duration.ofHours(24);
        }
        if (maxBodyBytes <= 0) {
            maxBodyBytes = 65536;
        }
        if (purgeInterval == null || purgeInterval.isZero() || purgeInterval.isNegative()) {
            purgeInterval = Duration.ofMinutes(30);
        }
    }
}
