// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_ai_knowledge")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledge extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 256)
    private String description;

    @Column(name = "embedding_model", length = 128)
    private String embeddingModel;

    @Column(name = "vector_store", length = 128)
    private String vectorStore;

    @Column(name = "chunk_size", nullable = false)
    private int chunkSize = 512;

    @Column(name = "chunk_overlap", nullable = false)
    private int chunkOverlap = 50;

    @Column(nullable = false)
    private boolean enabled = true;
}
