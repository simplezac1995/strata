// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A token bucket per client and rule.
 *
 * <p>A bucket refills continuously at {@code capacity / period} tokens per second and each accepted request takes
 * one, so a client that sends a short burst is served and a client that sends a steady flood is throttled — the
 * behaviour a fixed "N per minute" counter cannot give. Refilling happens when a bucket is used, which is why
 * there is no background thread here.
 *
 * <p>Memory is bounded on purpose. A flood from many distinct addresses is exactly the case a limiter must
 * survive, so when the map passes its ceiling idle buckets are dropped; if a genuine flood leaves nothing idle
 * to drop, the limiter <b>fails open</b> and logs it. Refusing traffic because the protection ran out of room
 * would turn a limiter into the outage.
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final Clock clock;
    private final int maxBuckets;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private volatile boolean overflowReported;

    public TokenBucketRateLimiter(Clock clock, int maxBuckets) {
        this.clock = clock;
        this.maxBuckets = maxBuckets;
    }

    /**
     * Consumes one token for {@code key}.
     *
     * @return whether the request may proceed, and when the client may retry if it may not
     */
    public Decision tryAcquire(String key, int capacity, Duration period) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= maxBuckets && !evictIdle(period)) {
                return Decision.unlimited();
            }
            bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, clock.millis()));
        }
        return bucket.tryConsume(capacity, period, clock.millis());
    }

    /** Tracked clients — the number an operator watches to know the limiter is not growing without bound. */
    public int trackedClients() {
        return buckets.size();
    }

    /**
     * Drops buckets that have been idle for a whole period.
     *
     * @return whether anything was dropped, and therefore whether room was made
     */
    private boolean evictIdle(Duration period) {
        long cutoff = clock.millis() - Math.max(period.toMillis(), 1_000L);
        boolean dropped = false;
        Iterator<Map.Entry<String, Bucket>> entries = buckets.entrySet().iterator();
        while (entries.hasNext()) {
            if (entries.next().getValue().lastSeenMillis() < cutoff) {
                entries.remove();
                dropped = true;
            }
        }
        if (!dropped && !overflowReported) {
            overflowReported = true;
            org.slf4j.LoggerFactory.getLogger(TokenBucketRateLimiter.class)
                    .warn(
                            "Rate limiter is tracking {} clients with none idle; allowing requests through unthrottled",
                            buckets.size());
        }
        return dropped;
    }

    /** The answer for one request. */
    public record Decision(boolean allowed, int remaining, long retryAfterSeconds) {

        /** Fail-open: nothing was throttled, so the client is not told to wait. */
        static Decision unlimited() {
            return new Decision(true, 0, 0);
        }
    }

    /** One client's tokens. Guarded by its own monitor: contention is per client, never global. */
    private static final class Bucket {

        private double tokens;
        private long lastRefillMillis;

        Bucket(int capacity, long nowMillis) {
            this.tokens = capacity;
            this.lastRefillMillis = nowMillis;
        }

        long lastSeenMillis() {
            return lastRefillMillis;
        }

        synchronized Decision tryConsume(int capacity, Duration period, long nowMillis) {
            double ratePerMillis = capacity / (double) Math.max(period.toMillis(), 1L);
            long elapsed = Math.max(nowMillis - lastRefillMillis, 0L);
            tokens = Math.min(capacity, tokens + (elapsed * ratePerMillis));
            lastRefillMillis = nowMillis;

            if (tokens >= 1d) {
                tokens -= 1d;
                return new Decision(true, (int) Math.floor(tokens), 0);
            }
            // Rounded up: telling a client to retry in 0 seconds invites an immediate retry.
            long waitSeconds = (long) Math.ceil((1d - tokens) / ratePerMillis / 1000d);
            return new Decision(false, 0, Math.max(waitSeconds, 1L));
        }
    }
}
