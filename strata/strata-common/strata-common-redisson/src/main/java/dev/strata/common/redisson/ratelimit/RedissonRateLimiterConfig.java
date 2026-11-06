// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.ratelimit;

import dev.strata.common.core.config.RateLimitAutoConfiguration;
import dev.strata.common.core.ratelimit.RateLimitProperties;
import dev.strata.common.core.ratelimit.RateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter;
import dev.strata.common.redisson.config.RedissonAutoConfiguration;
import java.time.Clock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swaps the in-memory rate limiter for a {@link RedissonRateLimiter} the moment the deployment actually has
 * distributed limiting wired:
 *
 * <ul>
 *   <li>{@code strata.data.redis.enabled=true} — the deployment-wide Redis switch (the same flag that turns on
 *       Redis-backed login lock, token blacklist and session stores); and</li>
 *   <li>a {@link RedissonClient} bean exists — i.e. Redisson itself is enabled
 *       ({@code strata.redisson.enabled=true}) and on the classpath.</li>
 * </ul>
 *
 * <p>When this config contributes the {@link RateLimiter} bean, {@link RateLimitAutoConfiguration}'s
 * {@code @ConditionalOnMissingBean(RateLimiter.class)} backs off, so the distributed implementation is injected
 * into the {@code RateLimitFilter}. Either condition failing — Redis switched off, or Redisson not on the
 * classpath / not enabled — leaves no bean here and the plain in-memory {@code TokenBucketRateLimiter} is used.
 * That is the safe default: unit tests and single-node dev need no Redis, and a deployment that sets the Redis
 * switch without wiring Redisson degrades to per-JVM buckets rather than failing boot.
 *
 * <p>This deliberately keys off the deployment's own Redis switch rather than a separate {@code
 * strata.rate-limit.impl}: once the whole app runs on Redis, the flood limiter should share that same budget
 * automatically instead of needing a second, easy-to-forget opt-in.
 */
@Configuration
@AutoConfigureAfter(RedissonAutoConfiguration.class)
@AutoConfigureBefore(RateLimitAutoConfiguration.class)
@ConditionalOnProperty(prefix = "strata.data.redis", name = "enabled", havingValue = "true")
@ConditionalOnBean(RedissonClient.class)
public class RedissonRateLimiterConfig {

    @Bean
    RateLimiter redissonRateLimiter(RedissonClient redissonClient, RateLimitProperties properties) {
        TokenBucketRateLimiter localFallback = new TokenBucketRateLimiter(Clock.systemUTC(), properties.maxBuckets());
        return new RedissonRateLimiter(redissonClient, localFallback);
    }
}
