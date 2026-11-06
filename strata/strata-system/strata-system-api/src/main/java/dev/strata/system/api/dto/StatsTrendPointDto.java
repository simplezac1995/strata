// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/** One daily bucket of a stats trend series. {@code date} is {@code yyyy-MM-dd} (UTC). */
public record StatsTrendPointDto(String date, long value) {}
