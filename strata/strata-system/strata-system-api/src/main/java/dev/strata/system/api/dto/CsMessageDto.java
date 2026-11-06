// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

/** One customer-service message. {@code attachments} is the opaque JSON array stored on the row, verbatim. */
public record CsMessageDto(
        String id,
        String conversationId,
        String senderType,
        String content,
        String attachments,
        Instant readAt,
        Instant createdAt) {}
