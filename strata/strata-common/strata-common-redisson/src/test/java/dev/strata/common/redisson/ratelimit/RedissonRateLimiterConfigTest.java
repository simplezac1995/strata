// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.core.config.RateLimitAutoConfiguration;
import dev.strata.common.core.ratelimit.RateLimiter;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("RedissonRateLimiter 自动装配条件切换")
class RedissonRateLimiterConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RateLimitAutoConfiguration.class, RedissonRateLimiterConfig.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Nested
    @DisplayName("Redis 启用 + RedissonClient 存在")
    class RedisEnabled {

        @Test
        @DisplayName("注入 RedissonRateLimiter，替代内存桶")
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
    @DisplayName("Redis 未启用")
    class RedisDisabled {

        @Test
        @DisplayName("回退内存 TokenBucketRateLimiter，不引入 Redisson")
        void fallsBackToInMemory() {
            runner.run(context -> {
                assertThat(context).hasSingleBean(RateLimiter.class);
                assertThat(context.getBean(RateLimiter.class)).isInstanceOf(TokenBucketRateLimiter.class);
            });
        }
    }

    @Nested
    @DisplayName("Redis 启用但无 RedissonClient")
    class RedisEnabledNoClient {

        @Test
        @DisplayName("RedissonClient 缺失时不强行分布式，安全回退内存桶")
        void failsClosedToMemory() {
            runner.withPropertyValues("strata.data.redis.enabled=true").run(context -> {
                assertThat(context).hasSingleBean(RateLimiter.class);
                assertThat(context.getBean(RateLimiter.class)).isInstanceOf(TokenBucketRateLimiter.class);
            });
        }
    }
}
