// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.FundFlowDto;
import dev.strata.system.ops.api.entity.FundFlow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FundFlowMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    @Mapping(target = "userId", expression = "java(row.getUserId() == null ? null : String.valueOf(row.getUserId()))")
    @Mapping(target = "bizId", expression = "java(row.getBizId() == null ? null : String.valueOf(row.getBizId()))")
    FundFlowDto toDto(FundFlow row);
}
