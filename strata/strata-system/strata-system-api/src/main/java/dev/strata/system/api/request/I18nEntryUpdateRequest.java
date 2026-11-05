// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Size;

public record I18nEntryUpdateRequest(@Size(max = 512) String zh, @Size(max = 512) String en) {}
