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

/** Persisted operation log row ({@code sys_operation_log}). Not the {@code @OperationLog} annotation. */
@Entity
@Table(
        name = "sys_operation_log",
        indexes = {
            @Index(name = "idx_ol_operator_id", columnList = "operator_id"),
            @Index(name = "idx_ol_module_name", columnList = "module_name"),
            @Index(name = "idx_ol_created_at", columnList = "created_at")
        })
@Data
public class OperationLog {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "module_name", nullable = false, length = 64)
    private String moduleName;

    @Column(name = "action_name", nullable = false, length = 32)
    private String actionName;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    @Column(length = 512)
    private String detail;

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
