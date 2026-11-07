// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiMcpServerCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 256) String url,
        @Size(max = 32) String authType,
        @Size(max = 256) String authToken,
        Boolean enabled) {}
