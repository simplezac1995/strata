// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.ratelimit;

import dev.strata.common.core.ratelimit.RateLimitProperties.FailureMode;
import dev.strata.common.core.ratelimit.RateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter.Decision;
import java.time.Clock;
import java.time.Duration;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Distributed rate limiter backed by Redisson's {@link RRateLimiter}.
 *
 * <p>Where the in-memory {@code TokenBucketRateLimiter} shares buckets only inside one JVM, this implementation
 * shares them across every instance pointed at the same Redis: the rate (capacity per period) is stored as a
 * Redisson rate limiter and {@code tryAcquire()} is a non-blocking, Redis-atomic permit grab. All instances
 * therefore agree on one budget per client — the missing piece behind the "single-JVM bucket" debt in the
 * security design.
 *
 * <p><b>Fail open.</b> A flood protector must never become the outage: if Redis is unreachable the request is
 * allowed through and a WARN is logged, exactly like the in-memory limiter's overflow behaviour.
 *
 * <p>The rate is re-applied per call with {@code trySetRate}, which is a no-op when it already matches, so an
 * operator changing {@code capacity}/{@code period} takes effect without a restart and without a Lua migration.
 */
public class RedissonRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedissonRateLimiter.class);

    /** Redis key prefix. The incoming {@code key} already embeds rule name and client identity. */
    static final String KEY_PREFIX = "strata:ratelimit:";

    private final RedissonClient redissonClient;
    private final TokenBucketRateLimiter localFallback;

    public RedissonRateLimiter(RedissonClient redissonClient) {
        this(redissonClient, new TokenBucketRateLimiter(Clock.systemUTC(), 100_000));
    }

    public RedissonRateLimiter(RedissonClient redissonClient, TokenBucketRateLimiter localFallback) {
        this.redissonClient = redissonClient;
        this.localFallback = localFallback;
    }

    @Override
    public Decision tryAcquire(String key, int capacity, Duration period) {
        return tryAcquire(key, capacity, period, FailureMode.FAIL_OPEN);
    }

    @Override
    public Decision tryAcquire(String key, int capacity, Duration period, FailureMode failureMode) {
        String name = KEY_PREFIX + key;
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(name);
            long intervalMillis = Math.max(period.toMillis(), 1L);
            // Idempotent: only (re)applies the rate when it is not already set to these values.
            limiter.trySetRate(RateType.OVERALL, capacity, intervalMillis, RateIntervalUnit.MILLISECONDS);

            boolean acquired = limiter.tryAcquire();
            if (acquired) {
                long available = limiter.availablePermits();
                int remaining = (int) Math.max(0L, Math.min(available, Integer.MAX_VALUE));
                return new Decision(true, remaining, 0L);
            }
            // Redisson does not expose "millis until the next permit"; a conservative one-window retry-after
            // keeps the client backing off for a full refill rather than hammering immediately.
            long retryAfter = Math.max(period.getSeconds(), 1L);
            return new Decision(false, 0, retryAfter);
        } catch (Exception e) {
            FailureMode mode = failureMode == null ? FailureMode.FAIL_OPEN : failureMode;
            log.warn(
                    "Redis distributed rate limiter failure for {}; degraded mode={}, error={}",
                    name,
                    mode,
                    e.getClass().getSimpleName());
            return switch (mode) {
                case LOCAL_FALLBACK -> localFallback.tryAcquire(key, capacity, period);
                case FAIL_CLOSED -> new Decision(false, 0, Math.max(period.getSeconds(), 1L));
                case FAIL_OPEN -> new Decision(true, 0, 0L);
            };
        }
    }
}
