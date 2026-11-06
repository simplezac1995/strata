// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.ReconciliationDailyDto;
import dev.strata.system.ops.api.entity.ReconciliationDaily;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReconciliationDailyMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    ReconciliationDailyDto toDto(ReconciliationDaily row);
}
