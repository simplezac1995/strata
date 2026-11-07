// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiPromptCreateRequest;
import dev.strata.ai.api.request.AiPromptUpdateRequest;
import dev.strata.ai.config.api.dto.AiPromptDto;
import dev.strata.ai.config.api.entity.AiPrompt;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiPromptMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiPrompt toEntity(AiPromptCreateRequest request);

    AiPromptDto toDto(AiPrompt prompt);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void update(AiPromptUpdateRequest request, @org.mapstruct.MappingTarget AiPrompt prompt);
}
