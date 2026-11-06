// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.math.BigDecimal;

public record AiBillingModelUsage(String model, long tokens, BigDecimal cost) {}
