// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Deterministic tests for the clock-rollback protection in {@link IdGenerator}.
 *
 * <p>The production class reads time through an injectable {@link Clock} (test seam
 * {@code useClock}); a queue-backed fake clock lets us move time backwards, stall, and
 * jump forward exactly on demand, so the rollback / wait branches run with no real sleep.
 * The base timestamp is chosen far in the future so it never collides with the real
 * wall-clock value left behind by the pre-existing {@link IdGeneratorTest}.
 */
class IdGeneratorRollbackTest {

    /**
     * Base epoch millis: above the real wall clock (~1.76e12 in 2026) so a fake clock never looks
     * like a spurious rollback, and far enough below EPOCH+2^41 (~3.90e12) to fit the 41-bit field.
     */
    private static final long B = 3_800_000_000_000L;

    @AfterEach
    void restoreClock() {
        IdGenerator.resetForTest();
    }

    /** A clock that pops a pre-recorded time on each call, then sticks on the last value. */
    private static final class FakeClock extends Clock {
        private final Deque<Long> queue = new ArrayDeque<>();
        private long last;

        FakeClock(long... times) {
            for (long t : times) {
                queue.add(t);
                last = t;
            }
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }

        @Override
        public long millis() {
            Long t = queue.poll();
            if (t != null) {
                last = t;
            }
            return last;
        }
    }

    @Test
    void smallRollbackWaitsAndResumes() {
        // next#1 at B; next#2 sees B-2 (2ms rollback) and the wait loop jumps to B+1.
        IdGenerator.useClock(new FakeClock(B, B - 2, B - 2, B - 2, B - 2, B + 1));

        IdGenerator.next();
        long second = IdGenerator.next();

        assertThat(IdGenerator.extractTimestamp(second)).isEqualTo(B + 1);
    }

    @Test
    void largeRollbackRefusesToGenerate() {
        IdGenerator.useClock(new FakeClock(B, B - 100));

        IdGenerator.next();

        assertThatThrownBy(IdGenerator::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refusing to generate ID");
    }

    @Test
    void waitTimeoutThrows() {
        // Roll back 2ms (small enough to enter the wait), then jump the clock past the 1000ms budget.
        IdGenerator.useClock(new FakeClock(B, B - 2, B - 2, B - 2, B + 1005));

        IdGenerator.next();

        assertThatThrownBy(IdGenerator::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Waited too long");
    }

    @Test
    void batchRejectsNonPositiveCount() {
        assertThatThrownBy(() -> IdGenerator.nextBatch(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdGenerator.nextBatch(-3)).isInstanceOf(IllegalArgumentException.class);
    }
}
