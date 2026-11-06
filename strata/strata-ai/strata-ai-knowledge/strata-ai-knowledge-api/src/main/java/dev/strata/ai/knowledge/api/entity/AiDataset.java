// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "sys_ai_dataset",
        indexes = {@Index(name = "idx_ai_dataset_created_at", columnList = "created_at")})
@Data
@EqualsAndHashCode(callSuper = true)
public class AiDataset extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 256)
    private String description;

    @Column(nullable = false, length = 32)
    private String source = "manual";

    @Column(nullable = false)
    private int records = 0;

    @Column(nullable = false)
    private boolean enabled = true;
}
