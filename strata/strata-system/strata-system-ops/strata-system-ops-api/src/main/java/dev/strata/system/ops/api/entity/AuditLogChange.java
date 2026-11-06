// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Per-field before/after change attached to a {@code sys_operation_log} row ({@code audit_log_change}).
 *
 * <p>The existing operation log only records who/when/what action; this table captures the actual old and new
 * values so a single field can be audited and (later) rolled back. This batch only ships query + a rollback
 * endpoint shell; automatic write-back is out of scope.
 */
@Entity
@Table(
        name = "audit_log_change",
        indexes = {
            @Index(name = "idx_alc_operation_log_id", columnList = "operation_log_id"),
            @Index(name = "idx_alc_entity", columnList = "entity_name, entity_id"),
            @Index(name = "idx_alc_created_at", columnList = "created_at")
        })
public class AuditLogChange extends TimestampedEntity {

    @Column(name = "operation_log_id")
    private Long operationLogId;

    @Column(name = "entity_name", nullable = false, length = 128)
    private String entityName;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "field_name", nullable = false, length = 128)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    public Long getOperationLogId() {
        return operationLogId;
    }

    public void setOperationLogId(Long operationLogId) {
        this.operationLogId = operationLogId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
