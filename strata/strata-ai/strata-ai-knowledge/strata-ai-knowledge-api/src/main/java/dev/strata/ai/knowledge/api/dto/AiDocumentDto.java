// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.dto;

import java.time.Instant;

public record AiDocumentDto(
        String id,
        String knowledgeId,
        String name,
        int sizeKb,
        String status,
        int chunks,
        boolean enabled,
        Instant createdAt) {}
