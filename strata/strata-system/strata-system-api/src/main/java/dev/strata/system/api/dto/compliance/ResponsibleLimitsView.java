// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.util.List;

/** Player's own view of responsible-gambling tools in force. */
public record ResponsibleLimitsView(
        List<ResponsibleGamblingLimitDto> limits, List<SelfExclusionDto> selfExclusions, List<CoolOffDto> coolOffs) {

    public ResponsibleLimitsView {
        if (limits == null) {
            limits = List.of();
        }
        if (selfExclusions == null) {
            selfExclusions = List.of();
        }
        if (coolOffs == null) {
            coolOffs = List.of();
        }
    }
}
