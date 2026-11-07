// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.core.config.RateLimitAutoConfiguration;
import dev.strata.common.core.ratelimit.RateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

/**
 * Proves the P0-2 wiring: the distributed {@link RedissonRateLimiter} is chosen only when Redis is enabled and a
 * {@link RedissonClient} bean exists; otherwise the in-JVM {@link TokenBucketRateLimiter} is the fallback.
 */
class RedissonRateLimiterConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RateLimitAutoConfiguration.class, RedissonRateLimiterConfig.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Nested
    class RedisEnabled {

        @Test
        void usesDistributedLimiter() {
            runner.withPropertyValues("strata.data.redis.enabled=true")
                    .withBean(RedissonClient.class, () -> org.mockito.Mockito.mock(RedissonClient.class))
                    .run(context -> {
                        assertThat(context).hasSingleBean(RateLimiter.class);
                        assertThat(context.getBean(RateLimiter.class)).isInstanceOf(RedissonRateLimiter.class);
                    });
        }
    }

    @Nested
    class RedisDisabled {

        @Test
        void fallsBackToInMemory() {
            runner.run(context -> {
                assertThat(context).hasSingleBean(RateLimiter.class);
                assertThat(context.getBean(RateLimiter.class)).isInstanceOf(TokenBucketRateLimiter.class);
            });
        }
    }

    @Nested
    class RedisEnabledNoClient {

        @Test
        void failsClosedToMemory() {
            runner.withPropertyValues("strata.data.redis.enabled=true").run(context -> {
                assertThat(context).hasSingleBean(RateLimiter.class);
                assertThat(context.getBean(RateLimiter.class)).isInstanceOf(TokenBucketRateLimiter.class);
            });
        }
    }
}
