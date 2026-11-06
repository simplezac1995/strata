// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.AuditLogChangeDto;
import dev.strata.system.ops.api.entity.AuditLogChange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogChangeMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    @Mapping(
            target = "operationLogId",
            expression = "java(row.getOperationLogId() == null ? null : String.valueOf(row.getOperationLogId()))")
    AuditLogChangeDto toDto(AuditLogChange row);
}
