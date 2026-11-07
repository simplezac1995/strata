// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPromptCreateRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 2000) String template,
        Boolean enabled) {}
