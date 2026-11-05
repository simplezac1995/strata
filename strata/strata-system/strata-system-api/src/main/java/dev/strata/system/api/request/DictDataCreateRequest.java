// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DictDataCreateRequest(
        @NotNull Long typeId,
        @NotBlank @Size(max = 128) String value,
        @NotBlank @Size(max = 128) String label,
        int sortOrder,
        boolean enabled) {}
