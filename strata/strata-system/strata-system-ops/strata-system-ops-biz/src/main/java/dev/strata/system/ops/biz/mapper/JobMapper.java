// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.JobDto;
import dev.strata.system.api.request.JobCreateRequest;
import dev.strata.system.api.request.JobUpdateRequest;
import dev.strata.system.ops.api.entity.SysJob;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PAUSED")
    @Mapping(target = "lastRun", ignore = true)
    @Mapping(target = "nextRun", ignore = true)
    @Mapping(target = "jobGroup", source = "jobGroup", defaultValue = "DEFAULT")
    @Mapping(target = "enabled", source = "enabled", defaultValue = "true")
    SysJob toEntity(JobCreateRequest request);

    JobDto toDto(SysJob job);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastRun", ignore = true)
    @Mapping(target = "nextRun", ignore = true)
    void update(JobUpdateRequest request, @org.mapstruct.MappingTarget SysJob job);
}
