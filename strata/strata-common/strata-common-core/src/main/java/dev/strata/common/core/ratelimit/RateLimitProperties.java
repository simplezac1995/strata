// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Flood control: how many requests a client may make to a path in a window.
 *
 * <p>The rules are policy and live in configuration, so this module stays free of any particular path. A
 * deployment that lists no rules gets a filter that matches nothing — the mechanism is here, the policy is
 * where the surface is defined.
 *
 * <p>This is deliberately <b>not</b> a product limit. Player betting limits, withdrawal caps and self-exclusion
 * are business rules that belong to the player's own state and are enforced in the domain; this is only about
 * refusing a flood cheaply, before an expensive operation (a BCrypt hash, a chain call) is attempted.
 */
@ConfigurationProperties(prefix = "strata.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        /**
         * Whether {@code @RateLimit}-annotated methods are throttled.
         *
         * <p>Separate from {@link #enabled} because the two answer different questions. {@code enabled} is a
         * deployment posture — the production guardrail insists the feature is on — while this one governs the
         * per-endpoint annotation buckets. An integration suite signs in hundreds of times from one loopback address
         * inside one JVM, so those annotations answer 429 from the second test onwards; switching only the annotations
         * off keeps the path-level limiter, and the guardrail, exactly as they are.
         */
        @DefaultValue("true") boolean annotationsEnabled,
        @DefaultValue({}) List<Rule> rules,
        /** How long an untouched bucket is remembered before it can be forgotten. */
        @DefaultValue("PT10M") Duration idleExpiry,
        /** Ceiling on tracked clients, so a flood of distinct addresses cannot grow the map without bound. */
        @DefaultValue("100000") int maxBuckets) {

    public RateLimitProperties {
        if (rules == null) {
            rules = List.of();
        }
        if (idleExpiry == null || idleExpiry.isZero() || idleExpiry.isNegative()) {
            idleExpiry = Duration.ofMinutes(10);
        }
        if (maxBuckets <= 0) {
            maxBuckets = 100_000;
        }
    }

    /**
     * One rule: {@code capacity} requests per {@code period} for requests matching {@code pathPattern} and
     * {@code methods}.
     *
     * @param name shown in the log line and used as the bucket prefix, so one client's login attempts and
     *     deposit attempts are counted separately
     * @param pathPattern Ant-style pattern, e.g. {@code /v1/auth/**}
     * @param capacity requests allowed inside one period
     * @param period window the capacity refills over
     */
    public record Rule(
            String name,
            String pathPattern,
            @DefaultValue({"POST"}) List<String> methods,
            @DefaultValue("30") int capacity,
            @DefaultValue("PT1M") Duration period,
            @DefaultValue("FAIL_OPEN") FailureMode failureMode) {

        /**
         * Convenience for callers that do not care about the failure mode.
         *
         * <p>A static factory rather than an overloaded constructor on purpose: a record with more than one
         * public constructor cannot be chosen by the configuration binder, and it fails the whole {@code rules}
         * list with "elements were left unbound" instead of saying which constructor it wanted. That exact
         * failure stopped the application from starting once and cost an afternoon; the factory keeps the
         * convenience without giving the binder a choice to get wrong.
         */
        public static Rule of(String name, String pathPattern, List<String> methods, int capacity, Duration period) {
            return new Rule(name, pathPattern, methods, capacity, period, FailureMode.FAIL_OPEN);
        }

        public Rule {
            if (name == null || name.isBlank()) {
                name = pathPattern;
            }
            if (methods == null || methods.isEmpty()) {
                methods = List.of("POST");
            }
            if (capacity <= 0) {
                capacity = 30;
            }
            if (period == null || period.isZero() || period.isNegative()) {
                period = Duration.ofMinutes(1);
            }
            if (failureMode == null) {
                failureMode = FailureMode.FAIL_OPEN;
            }
        }
    }

    /** Behavior when the distributed limiter cannot reach Redis. */
    public enum FailureMode {
        /** Preserve availability; only use for non-sensitive traffic. */
        FAIL_OPEN,
        /** Keep a per-process token bucket as a degraded guard. */
        LOCAL_FALLBACK,
        /** Reject the request until the distributed limiter recovers. */
        FAIL_CLOSED
    }
}
