// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

/**
 * Outcome of claiming an {@code Idempotency-Key}.
 *
 * <p>Sealed so the filter has to handle every case: a new claim may run the operation, a replay may not,
 * and the two failure modes (still running / key reused with another payload) are distinct answers.
 */
public sealed interface IdempotencyClaim {

    /** The key was free: this caller owns it and may run the operation. */
    record Claimed(long id) implements IdempotencyClaim {}

    /** The key already produced a response: hand that response back and skip the operation. */
    record Replay(StoredResponse response) implements IdempotencyClaim {}

    /** Another request with the same key is still running (double tap, or a retry after a timeout). */
    record InProgress() implements IdempotencyClaim {}

    /** The same key arrived with a different method/path/body: reusing a key that way is a client bug. */
    record Mismatch() implements IdempotencyClaim {}
}
