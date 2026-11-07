// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class IdempotencyPurgeJobTest {

    @Test
    void purgeDelegatesToStore() {
        IdempotencyStore store = mock(IdempotencyStore.class);
        when(store.purgeExpired(any(Instant.class))).thenReturn(3);

        new IdempotencyPurgeJob(store).purge();

        verify(store).purgeExpired(any(Instant.class));
    }

    @Test
    void purgeSwallowsStoreFailure() {
        IdempotencyStore store = mock(IdempotencyStore.class);
        doThrow(new RuntimeException("db down")).when(store).purgeExpired(any(Instant.class));

        new IdempotencyPurgeJob(store).purge();

        verify(store).purgeExpired(any(Instant.class));
    }
}
