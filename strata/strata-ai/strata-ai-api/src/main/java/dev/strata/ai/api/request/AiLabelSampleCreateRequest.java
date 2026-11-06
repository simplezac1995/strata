// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiLabelSampleCreateRequest(@NotBlank @Size(max = 2000) String content, @Size(max = 256) String label) {}
