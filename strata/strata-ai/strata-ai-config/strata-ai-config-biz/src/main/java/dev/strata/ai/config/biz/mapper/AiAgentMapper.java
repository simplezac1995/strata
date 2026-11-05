// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiAgentCreateRequest;
import dev.strata.ai.api.request.AiAgentUpdateRequest;
import dev.strata.ai.config.api.dto.AiAgentDto;
import dev.strata.ai.config.api.entity.AiAgent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiAgentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiAgent toEntity(AiAgentCreateRequest request);

    AiAgentDto toDto(AiAgent agent);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(AiAgentUpdateRequest request, @org.mapstruct.MappingTarget AiAgent agent);
}
