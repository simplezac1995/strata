// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Deletes idempotency records past their retention window. Without it the table grows without bound,
 * because every write endpoint with a key adds a row and nothing else ever removes one.
 *
 * <p>Requires {@code @EnableScheduling} in the application (strata-app enables it; a new module has to
 * do the same) — a missing scheduler simply means no purge, not a broken request path.
 */
public class IdempotencyPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyPurgeJob.class);

    private final IdempotencyStore store;

    public IdempotencyPurgeJob(IdempotencyStore store) {
        this.store = store;
    }

    @Scheduled(
            fixedDelayString = "${strata.idempotency.purge-interval:PT30M}",
            initialDelayString = "${strata.idempotency.purge-interval:PT30M}")
    public void purge() {
        try {
            store.purgeExpired(Instant.now());
        } catch (RuntimeException failure) {
            log.warn("idempotency purge failed; expired records stay until the next run", failure);
        }
    }
}
