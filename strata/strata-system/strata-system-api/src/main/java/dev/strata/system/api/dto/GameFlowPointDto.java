// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/** One day of the game-flow trend. Amounts are in minor units. */
public record GameFlowPointDto(
        String date, long gameBetAmountMinor, long gamePayoutAmountMinor, long gamePlayerCount) {}
