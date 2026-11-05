// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.mapper;

import dev.strata.ai.api.request.AiVectorStoreCreateRequest;
import dev.strata.ai.api.request.AiVectorStoreUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiVectorStoreDto;
import dev.strata.ai.knowledge.api.entity.AiVectorStore;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiVectorStoreMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiVectorStore toEntity(AiVectorStoreCreateRequest request);

    AiVectorStoreDto toDto(AiVectorStore vectorStore);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(AiVectorStoreUpdateRequest request, @org.mapstruct.MappingTarget AiVectorStore vectorStore);
}
