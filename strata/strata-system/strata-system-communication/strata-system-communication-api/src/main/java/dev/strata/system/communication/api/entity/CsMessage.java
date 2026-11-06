// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.api.entity;

import dev.strata.common.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/**
 * One message inside a {@link CsConversation} ({@code cs_message}). Append-only: a message is never updated,
 * only marked read, so it carries a single {@code created_at} and no audit columns. {@code sender_type} is
 * {@code player}, {@code staff} or {@code system}. {@code attachments} holds an opaque JSON array of attachment
 * descriptors for this batch; the shape is left to the client.
 */
@Entity
@Table(
        name = "cs_message",
        indexes = {@Index(name = "idx_cs_msg_conversation", columnList = "conversation_id")})
@Data
public class CsMessage {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_type", nullable = false, length = 16)
    private String senderType;

    @Column(length = 2000)
    private String content;

    @Column(length = 1024)
    private String attachments;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = IdGenerator.next();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
