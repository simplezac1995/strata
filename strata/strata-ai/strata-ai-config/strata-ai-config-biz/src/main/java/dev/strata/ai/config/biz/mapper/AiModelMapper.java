// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiModelCreateRequest;
import dev.strata.ai.api.request.AiModelUpdateRequest;
import dev.strata.ai.config.api.dto.AiModelDto;
import dev.strata.ai.config.api.entity.AiModel;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiModelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiModel toEntity(AiModelCreateRequest request);

    AiModelDto toDto(AiModel model);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "code", ignore = true)
    void update(AiModelUpdateRequest request, @org.mapstruct.MappingTarget AiModel model);
}
