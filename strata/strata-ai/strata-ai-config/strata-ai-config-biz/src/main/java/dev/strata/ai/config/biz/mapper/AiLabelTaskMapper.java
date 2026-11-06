// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiLabelTaskCreateRequest;
import dev.strata.ai.api.request.AiLabelTaskUpdateRequest;
import dev.strata.ai.config.api.dto.AiLabelTaskDto;
import dev.strata.ai.config.api.entity.AiLabelTask;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiLabelTaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sampleCount", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    AiLabelTask toEntity(AiLabelTaskCreateRequest request);

    AiLabelTaskDto toDto(AiLabelTask task);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sampleCount", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void update(AiLabelTaskUpdateRequest request, @org.mapstruct.MappingTarget AiLabelTask task);
}
