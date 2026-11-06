// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiModelCreateRequest(
        @NotNull Long providerId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 32) String type,
        Boolean enabled) {}
