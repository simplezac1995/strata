// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import java.time.Duration;

/**
 * Flood-control strategy: one token-bucket decision per request.
 *
 * <p>This is the seam between the <b>in-memory</b> {@link TokenBucketRateLimiter} (the default, no Redis
 * required, unit-test friendly) and a <b>distributed</b> implementation that shares buckets across instances
 * through Redis. The {@link RateLimitFilter} depends on this interface, so a deployment can swap the
 * implementation by contributing a different bean; the filter and its policy ({@link RateLimitProperties})
 * stay unchanged.
 *
 * <p>The decision type is deliberately the existing {@link TokenBucketRateLimiter.Decision}: it is the value
 * the filter already reads (allowed / remaining / retry-after), and keeping one type avoids a parallel
 * result hierarchy.
 */
public interface RateLimiter {

    /**
     * Consumes one token for {@code key}.
     *
     * @param key the bucket identity (rule name + client identity)
     * @param capacity requests allowed inside one period
     * @param period the window the capacity refills over
     * @return whether the request may proceed, and when the client may retry if it may not
     */
    TokenBucketRateLimiter.Decision tryAcquire(String key, int capacity, Duration period);

    /**
     * Consumes a permit with the policy-specific Redis outage behavior. Local implementations naturally ignore
     * the mode because they are already the fallback strategy.
     */
    default TokenBucketRateLimiter.Decision tryAcquire(
            String key, int capacity, Duration period, RateLimitProperties.FailureMode failureMode) {
        return tryAcquire(key, capacity, period);
    }
}
