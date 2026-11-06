// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(@Min(1) @Max(100_000_000) long amountCents, @Size(max = 8) String currency) {}
