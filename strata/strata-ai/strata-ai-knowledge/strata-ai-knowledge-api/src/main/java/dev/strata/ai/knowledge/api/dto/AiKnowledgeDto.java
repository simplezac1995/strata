// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.dto;

public record AiKnowledgeDto(
        String id,
        String name,
        String description,
        String embeddingModel,
        String vectorStore,
        int chunkSize,
        int chunkOverlap,
        boolean enabled) {}
