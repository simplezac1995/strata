// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Regression test for IdGenerator thread-safety.
 *
 * <p>Before the {@code synchronized} fix, two threads entering the "new
 * millisecond" branch concurrently could both reset the sequence to 0 and
 * return the same ID, surfacing as duplicate-key errors at ~0.1% under
 * concurrent user creation. This test hammers the generator from many threads
 * and asserts global uniqueness plus a monotone-ish timestamp layout.
 */
class IdGeneratorTest {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 5000;

    @Test
    void concurrentGenerationProducesNoDuplicates() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Long>>> futures = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                List<Long> ids = new ArrayList<>(PER_THREAD);
                for (int i = 0; i < PER_THREAD; i++) {
                    ids.add(IdGenerator.next());
                }
                return ids;
            }));
        }
        start.countDown();

        Set<Long> all = new HashSet<>(THREADS * PER_THREAD);
        for (Future<List<Long>> f : futures) {
            all.addAll(f.get());
        }
        pool.shutdown();

        assertThat(all).hasSize(THREADS * PER_THREAD);
    }

    @Test
    void idLayoutExtractsTimestampAndWorkerId() {
        long id = IdGenerator.next();
        long ts = IdGenerator.extractTimestamp(id);
        long worker = IdGenerator.extractWorkerId(id);
        long seq = IdGenerator.extractSequence(id);

        assertThat(ts).isGreaterThan(0);
        assertThat(worker).isBetween(0L, 1023L);
        assertThat(seq).isBetween(0L, 4095L);
    }

    @Test
    void batchGenerationIsUnique() {
        List<Long> ids = IdGenerator.nextBatch(1000);
        assertThat(new HashSet<>(ids)).hasSize(1000);
        List<Long> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        assertThat(sorted.get(0)).isLessThan(sorted.get(999));
    }
}
