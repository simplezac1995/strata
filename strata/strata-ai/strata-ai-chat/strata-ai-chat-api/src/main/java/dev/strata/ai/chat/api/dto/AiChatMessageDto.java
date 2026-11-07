// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.api.dto;

import java.time.Instant;

public record AiChatMessageDto(String id, String sessionId, String role, String content, Instant createdAt) {}
