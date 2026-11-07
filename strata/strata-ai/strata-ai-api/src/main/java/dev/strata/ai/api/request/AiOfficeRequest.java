// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiOfficeRequest(@NotBlank @Size(max = 8000) String text, @Size(max = 32) String tone) {}
