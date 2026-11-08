// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiSkillCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 16) String type,
        @Size(max = 128) String path,
        @Size(max = 256) String description,
        Boolean enabled) {}
