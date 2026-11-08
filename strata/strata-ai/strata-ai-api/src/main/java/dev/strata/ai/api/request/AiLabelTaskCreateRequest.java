// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiLabelTaskCreateRequest(@NotBlank @Size(max = 128) String name, @NotBlank @Size(max = 16) String type) {}
