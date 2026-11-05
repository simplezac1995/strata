// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CoolOffCreateRequest(@NotNull @Positive Integer hours) {}
