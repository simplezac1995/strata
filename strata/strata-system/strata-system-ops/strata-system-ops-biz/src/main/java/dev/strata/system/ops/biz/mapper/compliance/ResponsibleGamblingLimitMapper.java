// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.ResponsibleGamblingLimitDto;
import dev.strata.system.api.request.compliance.ResponsibleLimitCreateRequest;
import dev.strata.system.ops.api.entity.compliance.ResponsibleGamblingLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResponsibleGamblingLimitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    ResponsibleGamblingLimit toEntity(ResponsibleLimitCreateRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    @Mapping(target = "userId", expression = "java(row.getUserId() == null ? null : String.valueOf(row.getUserId()))")
    ResponsibleGamblingLimitDto toDto(ResponsibleGamblingLimit row);
}
