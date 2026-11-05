// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.api.dto;

import java.time.Instant;

public record AiChatSessionDto(String id, String title, String modelId, String userId, Instant createdAt) {}
