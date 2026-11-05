// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiDocumentCreateRequest(
        @NotNull Long knowledgeId,
        @NotBlank @Size(max = 128) String name,
        @Min(0) int sizeKb,
        @Size(max = 16) String status,
        @Min(0) int chunks,
        Boolean enabled) {}
