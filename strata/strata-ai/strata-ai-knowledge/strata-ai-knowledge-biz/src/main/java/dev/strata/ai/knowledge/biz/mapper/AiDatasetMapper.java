// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.mapper;

import dev.strata.ai.api.request.AiDatasetCreateRequest;
import dev.strata.ai.api.request.AiDatasetUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDatasetDto;
import dev.strata.ai.knowledge.api.entity.AiDataset;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiDatasetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiDataset toEntity(AiDatasetCreateRequest request);

    AiDatasetDto toDto(AiDataset dataset);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(AiDatasetUpdateRequest request, @org.mapstruct.MappingTarget AiDataset dataset);
}
