// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 64) String jobGroup,
        @NotBlank @Size(max = 64) String cron,
        Boolean enabled) {}
