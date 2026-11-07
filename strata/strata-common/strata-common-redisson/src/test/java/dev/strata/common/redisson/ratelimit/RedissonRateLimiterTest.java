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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
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
    void permit_exhausted_denies_with_one_window_retry_after() {
        when(rateLimiter.tryAcquire()).thenReturn(false);

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfterSeconds()).isEqualTo(PERIOD.getSeconds());
    }

    @Test
    void negative_available_permits_clamp_to_zero() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(rateLimiter.availablePermits()).thenReturn(-5L);

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isZero();
    }

    @Test
    void redis_exception_fails_open() {
        when(redissonClient.getRateLimiter(RATE_LIMITER_NAME)).thenThrow(new RuntimeException("Connection refused"));

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.retryAfterSeconds()).isZero();
    }

    @Test
    void redis_exception_uses_local_fallback_when_configured() {
        when(redissonClient.getRateLimiter(RATE_LIMITER_NAME)).thenThrow(new RuntimeException("Connection refused"));

        Decision first = limiter.tryAcquire(RULE_KEY, 1, PERIOD, FailureMode.LOCAL_FALLBACK);
        Decision second = limiter.tryAcquire(RULE_KEY, 1, PERIOD, FailureMode.LOCAL_FALLBACK);

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isFalse();
    }

    @Test
    void redis_exception_fails_closed_when_configured() {
        when(redissonClient.getRateLimiter(RATE_LIMITER_NAME)).thenThrow(new RuntimeException("Connection refused"));

        Decision decision = limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD, FailureMode.FAIL_CLOSED);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isEqualTo(PERIOD.getSeconds());
    }

    @Test
    void rate_limiter_name_uses_strata_prefix() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(rateLimiter.availablePermits()).thenReturn(10L);

        limiter.tryAcquire(RULE_KEY, CAPACITY, PERIOD);

        verify(redissonClient).getRateLimiter("strata:ratelimit:" + RULE_KEY);
        verify(rateLimiter, never())
                .trySetRate(eq(RateType.OVERALL), eq(0), anyLong(), eq(RateIntervalUnit.MILLISECONDS));
    }
}
