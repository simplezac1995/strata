// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.AmlReportDto;
import dev.strata.system.ops.api.entity.compliance.AmlReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AmlReportMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    @Mapping(target = "userId", expression = "java(row.getUserId() == null ? null : String.valueOf(row.getUserId()))")
    AmlReportDto toDto(AmlReport row);
}
