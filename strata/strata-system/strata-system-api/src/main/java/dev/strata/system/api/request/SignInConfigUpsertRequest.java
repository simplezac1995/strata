// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Full replacement of the per-day sign-in reward configuration.
 */
public record SignInConfigUpsertRequest(@NotEmpty @Valid List<Item> items) {

    public record Item(@Min(1) int day, @Min(0) long rewardPoints, BigDecimal rewardMinor) {}
}
