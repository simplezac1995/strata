// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import java.time.Instant;

/**
 * Storage for idempotency records. The default implementation is JDBC ({@link JdbcIdempotencyStore});
 * the interface stays narrow so a Redis-backed fast path can be added without touching the filter.
 *
 * <p>{@code claim} must be atomic across instances: the implementation relies on a unique index rather
 * than on in-process locking, because the whole point is protection against a second node accepting the
 * same retry.
 *
 * <p>A claim that never produces a response must be {@link #release(long) released} — otherwise a failed
 * attempt would block its key for the whole retention window and the client could never retry.
 */
public interface IdempotencyStore {

    /** Claims the key, replays a stored response, or reports a conflict. */
    IdempotencyClaim claim(IdempotencyRequest request);

    /** Stores the response for a claimed key and marks it completed. */
    void complete(long id, StoredResponse response);

    /** Drops an unfinished claim so the key becomes usable again. Completed records are never removed. */
    void release(long id);

    /** Deletes expired records; returns how many rows were removed. */
    int purgeExpired(Instant now);
}
