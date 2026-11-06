// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "ai_chat_session",
        indexes = {
            @Index(name = "idx_ai_chat_user_id", columnList = "user_id"),
            @Index(name = "idx_ai_chat_model_id", columnList = "model_id"),
            @Index(name = "idx_ai_chat_created_at", columnList = "created_at"),
            @Index(name = "idx_ai_chat_user_created", columnList = "user_id, created_at")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatSession extends AuditingEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
