// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RunReconciliationRequest(@NotNull LocalDate date) {}
