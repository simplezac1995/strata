// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiKnowledgeCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 256) String description,
        @Size(max = 128) String embeddingModel,
        @Size(max = 128) String vectorStore,
        @Min(0) int chunkSize,
        @Min(0) int chunkOverlap,
        Boolean enabled) {}
