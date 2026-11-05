// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiVectorStoreUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 32) String type,
        @Size(max = 256) String host,
        @Min(0) int dimensions,
        Boolean enabled) {}
