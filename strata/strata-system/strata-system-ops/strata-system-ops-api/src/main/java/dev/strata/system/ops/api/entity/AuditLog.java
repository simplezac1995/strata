// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

@Entity
@Table(
        name = "sys_audit_log",
        indexes = {
            @Index(name = "idx_al_operator_id", columnList = "operator_id"),
            @Index(name = "idx_al_action_name", columnList = "action_name"),
            @Index(name = "idx_al_resource_type", columnList = "resource_type"),
            @Index(name = "idx_al_created_at", columnList = "created_at"),
            @Index(name = "idx_al_operator_created", columnList = "operator_id, created_at")
        })
@Data
public class AuditLog {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "module_name", nullable = false, length = 64)
    private String moduleName;

    @Column(name = "action_name", nullable = false, length = 32)
    private String actionName;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(nullable = false)
    private boolean success = true;

    @Column(length = 512)
    private String detail;

    @Column(name = "changed_fields", columnDefinition = "TEXT")
    private String changedFields;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = IdGenerator.next();
        }
        createdAt = Instant.now();
    }
}
