// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * The limiter's arithmetic and its failure mode. Time is supplied by a clock the test moves, so a one-minute
 * refill is asserted without a one-minute test.
 */
class TokenBucketRateLimiterTest {

    private static final class TestClock extends Clock {

        private Instant now = Instant.parse("2026-09-13T12:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        void advance(Duration amount) {
            now = now.plus(amount);
        }
    }

    private final TestClock clock = new TestClock();
    private final TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(clock, 10);

    @Test
    void serves_a_burst_up_to_the_capacity_then_refuses() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("ip|1.2.3.4", 5, Duration.ofMinutes(1))
                            .allowed())
                    .as("request %s is inside the burst", i + 1)
                    .isTrue();
        }

        TokenBucketRateLimiter.Decision refused = limiter.tryAcquire("ip|1.2.3.4", 5, Duration.ofMinutes(1));
        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds())
                .as("a client told to retry in 0 seconds retries immediately")
                .isGreaterThanOrEqualTo(1L);
    }

    @Test
    void refills_continuously_rather_than_resetting_on_a_window_boundary() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("ip|1.2.3.4", 5, Duration.ofMinutes(1));
        }
        assertThat(limiter.tryAcquire("ip|1.2.3.4", 5, Duration.ofMinutes(1)).allowed())
                .isFalse();

        // 30 seconds into a 1-minute window: half the capacity is back.
        clock.advance(Duration.ofSeconds(30));

        assertThat(limiter.tryAcquire("ip|1.2.3.4", 5, Duration.ofMinutes(1)).allowed())
                .as("a steady client recovers without waiting for a boundary")
                .isTrue();
    }

    @Test
    void counts_each_client_and_each_rule_separately() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("login|1.2.3.4", 5, Duration.ofMinutes(1));
        }

        assertThat(limiter.tryAcquire("login|1.2.3.4", 5, Duration.ofMinutes(1)).allowed())
                .isFalse();
        assertThat(limiter.tryAcquire("payment|1.2.3.4", 5, Duration.ofMinutes(1))
                        .allowed())
                .as("one endpoint's budget does not spend another's")
                .isTrue();
        assertThat(limiter.tryAcquire("login|5.6.7.8", 5, Duration.ofMinutes(1)).allowed())
                .as("one client's flood does not throttle another")
                .isTrue();
    }

    @Test
    void stays_bounded_and_fails_open_when_there_is_no_room_left() {
        TokenBucketRateLimiter tiny = new TokenBucketRateLimiter(clock, 2);
        assertThat(tiny.tryAcquire("login|1.1.1.1", 5, Duration.ofMinutes(1)).allowed())
                .isTrue();
        assertThat(tiny.tryAcquire("login|2.2.2.2", 5, Duration.ofMinutes(1)).allowed())
                .isTrue();
        assertThat(tiny.trackedClients()).isEqualTo(2);

        TokenBucketRateLimiter.Decision decision = tiny.tryAcquire("login|3.3.3.3", 5, Duration.ofMinutes(1));

        assertThat(decision.allowed())
                .as("refusing traffic because the limiter ran out of room would be the outage")
                .isTrue();
        assertThat(tiny.trackedClients()).as("the map did not grow").isEqualTo(2);
    }

    @Test
    void forgets_a_client_once_it_has_been_idle_for_a_window() {
        TokenBucketRateLimiter tiny = new TokenBucketRateLimiter(clock, 1);
        tiny.tryAcquire("login|1.1.1.1", 5, Duration.ofMinutes(1));
        clock.advance(Duration.ofMinutes(2));

        assertThat(tiny.tryAcquire("login|2.2.2.2", 5, Duration.ofMinutes(1)).allowed())
                .isTrue();
        assertThat(tiny.trackedClients())
                .as("the idle bucket was dropped to make room, not bypassed")
                .isEqualTo(1);
    }
}
