// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParamCreateRequest(
        @NotBlank @Size(max = 128) String key,
        @NotBlank @Size(max = 512) String value,
        @Size(max = 32) String type,
        @Size(max = 256) String remark) {}
