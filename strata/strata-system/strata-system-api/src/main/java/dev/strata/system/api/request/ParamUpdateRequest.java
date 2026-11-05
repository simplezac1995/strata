// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParamUpdateRequest(
        @NotBlank @Size(max = 512) String value,
        @NotBlank @Size(max = 32) String type,
        @Size(max = 256) String remark) {}
