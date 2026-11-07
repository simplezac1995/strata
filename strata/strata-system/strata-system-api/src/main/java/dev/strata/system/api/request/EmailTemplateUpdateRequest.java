// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailTemplateUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 128) String subject,
        @Size(max = 2000) String body,
        Boolean enabled) {}
