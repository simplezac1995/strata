// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.DailyTaskDto;
import dev.strata.system.api.request.DailyTaskUpsertRequest;
import dev.strata.system.ops.api.entity.DailyTask;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DailyTaskMapper {

    @Mapping(target = "id", ignore = true)
    DailyTask toEntity(DailyTaskUpsertRequest request);

    DailyTaskDto toDto(DailyTask task);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(DailyTaskUpsertRequest request, @org.mapstruct.MappingTarget DailyTask task);
}
