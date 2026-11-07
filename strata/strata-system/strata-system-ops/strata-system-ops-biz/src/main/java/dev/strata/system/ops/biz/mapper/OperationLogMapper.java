// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.OperationLogDto;
import dev.strata.system.ops.api.entity.OperationLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OperationLogMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(record.getId()))")
    OperationLogDto toDto(OperationLog record);
}
