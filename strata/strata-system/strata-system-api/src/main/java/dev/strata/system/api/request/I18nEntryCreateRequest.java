// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record I18nEntryCreateRequest(
        @NotBlank @Size(max = 128) String messageKey, @Size(max = 512) String zh, @Size(max = 512) String en) {}
