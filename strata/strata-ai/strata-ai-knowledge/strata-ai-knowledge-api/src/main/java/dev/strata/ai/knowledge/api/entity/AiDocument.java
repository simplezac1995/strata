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
        name = "sys_ai_document",
        indexes = {
            @Index(name = "idx_ai_doc_knowledge_id", columnList = "knowledge_id"),
            @Index(name = "idx_ai_doc_status", columnList = "status"),
            @Index(name = "idx_ai_doc_created_at", columnList = "created_at")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class AiDocument extends AuditingEntity {

    @Column(name = "knowledge_id", nullable = false)
    private Long knowledgeId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "size_kb", nullable = false)
    private int sizeKb = 0;

    @Column(nullable = false, length = 16)
    private String status = "pending";

    @Column(nullable = false)
    private int chunks = 0;

    @Column(nullable = false)
    private boolean enabled = true;
}
