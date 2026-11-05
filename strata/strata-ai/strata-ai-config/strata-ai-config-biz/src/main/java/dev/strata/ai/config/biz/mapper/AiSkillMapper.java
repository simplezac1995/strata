// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiSkillCreateRequest;
import dev.strata.ai.api.request.AiSkillUpdateRequest;
import dev.strata.ai.config.api.dto.AiSkillDto;
import dev.strata.ai.config.api.entity.AiSkill;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiSkillMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", defaultValue = "skill")
    @Mapping(target = "enabled", defaultValue = "true")
    AiSkill toEntity(AiSkillCreateRequest request);

    AiSkillDto toDto(AiSkill skill);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    void update(AiSkillUpdateRequest request, @org.mapstruct.MappingTarget AiSkill skill);
}
