// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

/**
 * Daily time series for a single metric (#6), with the current window and the immediately preceding window
 * of equal length for comparison.
 */
public record StatsTrendDto(
        String metric, int days, List<StatsTrendPointDto> current, List<StatsTrendPointDto> previous) {

    public StatsTrendDto {
        if (current == null) {
            current = List.of();
        }
        if (previous == null) {
            previous = List.of();
        }
    }
}
