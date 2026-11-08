// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

/**
 * Customer-service conversation view. {@code playerId}/{@code staffId} are rendered as strings because Long
 * ids are serialized as strings end-to-end (JS cannot safely hold 64-bit ids).
 */
public record ConversationDto(
        String id,
        String playerId,
        String staffId,
        String status,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt) {}
