// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base for a state machine: the row carries a {@code version} that Hibernate checks on every update, so two
 * concurrent transitions cannot both win 閳?the loser gets an {@code OptimisticLockException} instead of
 * silently overwriting the winner.
 *
 * <p>Deliberately *not* used for balances. A wallet row is updated constantly by atomic increments
 * ({@code balance = balance + ?}), where a version column would turn normal concurrency into retries; the
 * ledger's own constraints (non-negative CHECK, unique entry number) are what protect that path.
 */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class VersionedEntity extends TimestampedEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
