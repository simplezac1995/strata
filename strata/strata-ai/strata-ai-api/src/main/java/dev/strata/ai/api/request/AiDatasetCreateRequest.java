// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiDatasetCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 256) String description,
        @NotBlank @Size(max = 32) String source,
        @Min(0) int records,
        Boolean enabled) {}
