// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAgentCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 64) String model,
        @Size(max = 2000) String prompt,
        Boolean enabled) {}
