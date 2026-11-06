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
        name = "ai_chat_message",
        indexes = {
            @Index(name = "idx_ai_msg_session_id", columnList = "session_id"),
            @Index(name = "idx_ai_msg_role", columnList = "role"),
            @Index(name = "idx_ai_msg_created_at", columnList = "created_at"),
            @Index(name = "idx_ai_msg_session_created", columnList = "session_id, created_at")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatMessage extends AuditingEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** user | assistant | system */
    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, length = 8000)
    private String content;
}
