// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.math.BigDecimal;

public record SignInConfigDto(String id, int day, long rewardPoints, BigDecimal rewardMinor) {}
