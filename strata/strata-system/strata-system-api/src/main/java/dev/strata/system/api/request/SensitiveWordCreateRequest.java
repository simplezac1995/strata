// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SensitiveWordCreateRequest(@NotBlank @Size(max = 128) String word, Boolean enabled) {}
