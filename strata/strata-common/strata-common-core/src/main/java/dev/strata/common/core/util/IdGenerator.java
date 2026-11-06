// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.util;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snowflake-style distributed ID generator with clock rollback protection.
 *
 * <p>ID structure (64 bits):
 * <ul>
 *   <li>1 bit: sign (always 0)</li>
 *   <li>41 bits: timestamp (milliseconds since epoch, ~69 years)</li>
 *   <li>10 bits: worker ID (0-1023)</li>
 *   <li>12 bits: sequence (0-4095 per millisecond)</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Clock rollback protection: waits for clock to catch up, or throws if rollback too large</li>
 *   <li>Batch ID generation for high-throughput scenarios</li>
 *   <li>Configurable worker ID via system property or environment variable</li>
 *   <li>Thread-safe using AtomicLong</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * long id = IdGenerator.next();
 * List<Long> ids = IdGenerator.nextBatch(100);
 * }</pre>
 */
public final class IdGenerator {

    private static final Logger log = LoggerFactory.getLogger(IdGenerator.class);

    // 2024-01-01 UTC
    private static final long EPOCH = 1_704_067_200_000L;

    // Bit allocations
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    // Masks
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // 1023
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS); // 4095

    // Shifts
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // Max clock rollback tolerance (5 milliseconds)
    private static final long MAX_CLOCK_ROLLBACK = 5L;

    // Max wait time for clock to catch up (1000 milliseconds)
    private static final long MAX_WAIT_TIME = 1000L;

    private static final AtomicLong SEQUENCE = new AtomicLong(0L);
    private static final long WORKER_ID = resolveWorkerId();

    // Last timestamp used for sequence generation
    private static volatile long lastTimestamp = -1L;

    // Test seam: injectable clock so rollback/timeout branches are deterministic. Default equals
    // System.currentTimeMillis().
    private static volatile Clock clock = Clock.systemUTC();

    private IdGenerator() {}

    /**
     * Generate a unique ID.
     *
     * @return a unique 64-bit ID
     * @throws IllegalStateException if clock rollback exceeds tolerance
     */
    public static synchronized long next() {
        long currentTimestamp = timeGen();

        // Clock rollback detection
        if (currentTimestamp < lastTimestamp) {
            long rollback = lastTimestamp - currentTimestamp;
            if (rollback <= MAX_CLOCK_ROLLBACK) {
                // Small rollback: wait for clock to catch up
                log.debug("Clock rolled back by {}ms, waiting...", rollback);
                currentTimestamp = waitUntilNextMillis(lastTimestamp);
            } else {
                // Large rollback: refuse to generate IDs to avoid duplicates
                throw new IllegalStateException(String.format(
                        "Clock rolled back by %dms, refusing to generate ID to avoid duplicates", rollback));
            }
        }

        // Same millisecond: increment sequence
        if (currentTimestamp == lastTimestamp) {
            long sequence = (SEQUENCE.incrementAndGet()) & SEQUENCE_MASK;
            if (sequence == 0) {
                // Sequence exhausted: wait for next millisecond
                currentTimestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            // New millisecond: reset sequence
            SEQUENCE.set(0L);
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | (SEQUENCE.get() & SEQUENCE_MASK);
    }

    /**
     * Generate a batch of unique IDs.
     *
     * @param count number of IDs to generate (must be positive)
     * @return list of unique IDs
     * @throws IllegalArgumentException if count is not positive
     * @throws IllegalStateException if clock rollback exceeds tolerance
     */
    public static List<Long> nextBatch(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive, got: " + count);
        }
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(next());
        }
        return ids;
    }

    /**
     * Get the configured worker ID.
     *
     * @return worker ID (0-1023)
     */
    public static long getWorkerId() {
        return WORKER_ID;
    }

    /**
     * Extract timestamp from a generated ID.
     *
     * @param id the generated ID
     * @return timestamp in milliseconds since epoch
     */
    public static long extractTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }

    /**
     * Extract worker ID from a generated ID.
     *
     * @param id the generated ID
     * @return worker ID (0-1023)
     */
    public static long extractWorkerId(long id) {
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * Extract sequence from a generated ID.
     *
     * @param id the generated ID
     * @return sequence number (0-4095)
     */
    public static long extractSequence(long id) {
        return id & SEQUENCE_MASK;
    }

    /**
     * Wait until the next millisecond.
     *
     * @param lastTimestamp the last timestamp used
     * @return the next timestamp
     */
    private static long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        long startTime = clock.millis();
        while (timestamp <= lastTimestamp) {
            if (clock.millis() - startTime > MAX_WAIT_TIME) {
                throw new IllegalStateException(
                        String.format("Waited too long (%dms) for next millisecond", MAX_WAIT_TIME));
            }
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * Get current timestamp.
     *
     * @return current time in milliseconds
     */
    private static long timeGen() {
        return clock.millis();
    }

    /** Test seam: override the clock (e.g. to simulate rollback). Passing null restores the system clock. */
    static void useClock(Clock newClock) {
        clock = newClock != null ? newClock : Clock.systemUTC();
    }

    /** Test seam: restore the default clock and clear generator state so fake-clock tests do not leak. */
    static void resetForTest() {
        clock = Clock.systemUTC();
        lastTimestamp = -1L;
        SEQUENCE.set(0L);
    }

    /**
     * Resolve worker ID from system property or environment variable.
     * Falls back to 1 if not configured.
     *
     * @return resolved worker ID
     */
    private static long resolveWorkerId() {
        String raw = System.getProperty("STRATA_WORKER_ID");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("STRATA_WORKER_ID");
        }
        if (raw == null || raw.isBlank()) {
            log.info("STRATA_WORKER_ID not set, using default worker ID: 1");
            return 1L;
        }
        try {
            long workerId = Long.parseLong(raw.trim());
            if (workerId < 0 || workerId > MAX_WORKER_ID) {
                log.warn("STRATA_WORKER_ID '{}' out of range [0,{}], falling back to 1", raw, MAX_WORKER_ID);
                return 1L;
            }
            log.info("Using worker ID: {} from STRATA_WORKER_ID", workerId);
            return workerId;
        } catch (NumberFormatException ex) {
            log.warn("STRATA_WORKER_ID '{}' is not a valid number, falling back to 1", raw);
            return 1L;
        }
    }
}
