// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiTermUpdateRequest(
        @NotBlank @Size(max = 128) String translation, @Size(max = 256) String notes, Boolean enabled) {}
