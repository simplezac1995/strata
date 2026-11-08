// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.entity;

import dev.strata.common.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import lombok.Data;

/**
 * Base for rows that are identified by a generated id and carry their own timestamps.
 *
 * <p>{@link AuditingEntity} adds the staff operator columns, {@link VersionedEntity} adds optimistic locking.
 * Player-facing rows want neither a staff operator nor the answer "who edited this" 鈥?they want the owning
 * aggregate's id 鈥?so they build on this class directly.
 */
@MappedSuperclass
@Data
public abstract class TimestampedEntity {

    @jakarta.persistence.Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = IdGenerator.next();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
