// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Staff-side customer-service conversation ({@code cs_conversation}). Players open a conversation from the
 * H5 player face; staff pick it up and reply on the admin workbench. {@code status} is an open enum encoded as
 * a short string: {@code open} (waiting for staff), {@code assigned} (a staff member is on it) or {@code
 * closed}. Messages live in {@link CsMessage}.
 */
@Entity
@Table(
        name = "cs_conversation",
        indexes = {
            @Index(name = "idx_cs_conv_status", columnList = "status"),
            @Index(name = "idx_cs_conv_player", columnList = "player_id"),
            @Index(name = "idx_cs_conv_staff", columnList = "staff_id")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class CsConversation extends AuditingEntity {

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(nullable = false)
    private boolean deleted = false;
}
