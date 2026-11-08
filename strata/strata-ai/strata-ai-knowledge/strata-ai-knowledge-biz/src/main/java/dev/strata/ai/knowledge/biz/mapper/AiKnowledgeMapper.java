// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.mapper;

import dev.strata.ai.api.request.AiKnowledgeCreateRequest;
import dev.strata.ai.api.request.AiKnowledgeUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiKnowledgeDto;
import dev.strata.ai.knowledge.api.entity.AiKnowledge;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiKnowledgeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiKnowledge toEntity(AiKnowledgeCreateRequest request);

    AiKnowledgeDto toDto(AiKnowledge knowledge);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(AiKnowledgeUpdateRequest request, @org.mapstruct.MappingTarget AiKnowledge knowledge);
}
