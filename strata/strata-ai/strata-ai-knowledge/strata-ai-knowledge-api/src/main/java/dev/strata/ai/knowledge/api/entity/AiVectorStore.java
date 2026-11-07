// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_ai_vector_store")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiVectorStore extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(length = 256)
    private String host;

    @Column(nullable = false)
    private int dimensions = 0;

    @Column(nullable = false)
    private boolean enabled = true;
}
