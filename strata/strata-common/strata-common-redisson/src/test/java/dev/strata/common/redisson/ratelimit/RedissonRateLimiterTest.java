// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.ratelimit.RateLimitProperties.FailureMode;
import dev.strata.common.core.ratelimit.TokenBucketRateLimiter.Decision;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedissonRateLimiter 分布式限流策略")
class RedissonRateLimiterTest {

    private static final String RULE_KEY = "login|10.0.0.1";
    private static final String RATE_LIMITER_NAME = RedissonRateLimiter.KEY_PREFIX + RULE_KEY;
    private static final int CAPACITY = 10;
    private static final Duration PERIOD = Duration.ofMinutes(1);

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter rateLimiter;

    private RedissonRateLimiter limiter;

    @BeforeEach
    void setUp() {
        when(redissonClient.getRateLimiter(RATE_LIMITER_NAME)).thenReturn(rateLimiter);
        limiter = new RedissonRateLimiter(redissonClient);
    }

    @Test
    @DisplayName("拿到许可：放行，remaining 来自 availablePermits，无 retryAfter")
    void permit_granted_allows_and_reports_remaining() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(rateLimiter.availablePermits()).thenReturn(7L);

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(7);
        assertThat(decision.retryAfterSeconds()).isZero();
        verify(rateLimiter).trySetRate(RateType.OVERALL, CAPACITY, PERIOD.toMillis(), RateIntervalUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("许可耗尽：拒绝，retryAfter 取一个周期")
    void permit_exhausted_denies_with_one_window_retry_after() {
        when(rateLimiter.tryAcquire()).thenReturn(false);

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfterSeconds()).isEqualTo(PERIOD.getSeconds());
    }

    @Test
    @DisplayName("availablePermits 为负时 remaining 归零，不为负")
    void negative_available_permits_clamp_to_zero() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(rateLimiter.availablePermits()).thenReturn(-5L);

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isZero();
    }

    @Test
    @DisplayName("Redis 异常时 fail-open 放行，不抛错")
    void redis_exception_fails_open() {
        when(redissonClient.getRateLimiter(RATE_LIMITER_NAME)).thenThrow(new RuntimeException("Connection refused"));

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.retryAfterSeconds()).isZero();
    }

    @Test
    @DisplayName("敏感规则在 Redis 故障时退化到本机桶")
    void redis_exception_uses_local_fallback_when_configured() {
        when(redissonClient.getRateLimiter(RATE_LIMITER_NAME)).thenThrow(new RuntimeException("Connection refused"));

        Decision first = limiter.tryAcquire(RULE_KEY, 1, PERIOD, FailureMode.LOCAL_FALLBACK);
        Decision second = limiter.tryAcquire(RULE_KEY, 1, PERIOD, FailureMode.LOCAL_FALLBACK);

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isFalse();
    }

    @Test
    @DisplayName("Redis 故障时 fail-closed 规则返回 429 决策")
    void redis_exception_fails_closed_when_configured() {
        when(redissonClient.getRateLimiter(RATE_LIMITER_NAME)).thenThrow(new RuntimeException("Connection refused"));

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD, FailureMode.FAIL_CLOSED);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isEqualTo(PERIOD.getSeconds());
    }

    @Test
    @DisplayName("Redis key 使用 strata:ratelimit: 前缀")
    void rate_limiter_name_uses_strata_prefix() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(rateLimiter.availablePermits()).thenReturn(10L);

        limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        verify(redissonClient).getRateLimiter("strata:ratelimit:" + RULE_KEY);
        verify(rateLimiter, never())
                .trySetRate(eq(RateType.OVERALL), eq(0), anyLong(), eq(RateIntervalUnit.MILLISECONDS));
    }
}
