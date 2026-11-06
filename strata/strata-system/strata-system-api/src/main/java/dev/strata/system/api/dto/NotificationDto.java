// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

/** One row of a staff member's notification inbox. */
public record NotificationDto(
        String id,
        String recipientUserId,
        String category,
        String title,
        String content,
        String actionUrl,
        Instant readAt,
        Instant createdAt) {}
