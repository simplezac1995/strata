// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.RiskAlertDto;
import dev.strata.system.ops.api.entity.RiskAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskAlertMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(alert.getId()))")
    @Mapping(
            target = "userId",
            expression = "java(alert.getUserId() == null ? null : String.valueOf(alert.getUserId()))")
    RiskAlertDto toDto(RiskAlert alert);
}
