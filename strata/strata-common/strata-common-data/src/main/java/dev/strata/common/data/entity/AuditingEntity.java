// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Staff-operated row: adds the operator columns on top of {@link TimestampedEntity}, so an admin change can
 * always be traced back to a {@code sys_user} id. Player-facing tables have no staff operator to record and
 * build on {@link TimestampedEntity} or {@link VersionedEntity} instead.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditingEntity extends TimestampedEntity {

    @CreatedBy
    @Column(name = "created_by")
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
