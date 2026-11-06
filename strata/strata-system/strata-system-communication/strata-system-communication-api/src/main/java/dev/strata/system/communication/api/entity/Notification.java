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
 * A single row in one staff member's notification inbox ({@code notification}). Broadcasts fan out into one
 * row per recipient; a row is addressed to exactly one {@code recipient_user_id}. {@code category} is one of
 * {@code system}, {@code security} or {@code business}. The row is append-only (marking it read only sets
 * {@code read_at}), so it carries a single {@code created_at}.
 */
@Entity
@Table(
        name = "notification",
        indexes = {
            @Index(name = "idx_notif_recipient", columnList = "recipient_user_id"),
            @Index(name = "idx_notif_recipient_unread", columnList = "recipient_user_id, read_at")
        })
@Data
public class Notification {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 16)
    private String category;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column(name = "action_url", length = 512)
    private String actionUrl;

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
