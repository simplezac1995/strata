// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting on a controller method or class.
 *
 * <p>Supports three rate limiting dimensions:
 * <ul>
 *   <li><b>Global</b>: limit applies to all requests (perIp=false, perUser=false)</li>
 *   <li><b>Per IP</b>: limit applies per client IP address (perIp=true, default)</li>
 *   <li><b>Per User</b>: limit applies per authenticated user (perUser=true)</li>
 * </ul>
 *
 * <p>Multiple dimensions can be combined, e.g., perIp=true and perUser=true creates
 * a separate bucket for each (IP, user) combination.
 *
 * <p>Usage:
 * <pre>{@code
 * // Per IP: 10 requests per 60 seconds
 * @RateLimit(limit = 10, window = 60, key = "login")
 * @PostMapping("/login")
 * public TokenResponse login(...) { ... }
 *
 * // Per User: 100 requests per 60 seconds
 * @RateLimit(limit = 100, window = 60, perUser = true, perIp = false)
 * @GetMapping("/api/data")
 * public DataResponse getData(...) { ... }
 *
 * // Global: 1000 requests per 60 seconds (all users share)
 * @RateLimit(limit = 1000, window = 60, perIp = false, perUser = false)
 * @PostMapping("/api/public")
 * public Response publicEndpoint(...) { ... }
 * }</pre>
 *
 * <p>Note: This implementation uses in-memory token buckets. For multi-instance
 * deployments, use a Redis-backed rate limiter for accurate global limits.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Maximum number of requests allowed in the time window. */
    int limit() default 100;

    /** Time window in seconds. */
    int window() default 60;

    /**
     * Rate limit key. If empty, the request URI is used.
     * Use a shared key to group multiple endpoints under the same limit.
     */
    String key() default "";

    /** Whether to include client IP in the rate limit key. Defaults to true. */
    boolean perIp() default true;

    /**
     * Whether to include authenticated user ID in the rate limit key. Defaults to false.
     * If true and no user is authenticated, falls back to "anonymous".
     */
    boolean perUser() default false;
}
