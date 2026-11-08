// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.mapper;

import dev.strata.ai.api.request.AiDocumentCreateRequest;
import dev.strata.ai.api.request.AiDocumentUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDocumentDto;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", defaultValue = "pending")
    @Mapping(target = "enabled", defaultValue = "true")
    AiDocument toEntity(AiDocumentCreateRequest request);

    AiDocumentDto toDto(AiDocument document);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "knowledgeId", ignore = true)
    void update(AiDocumentUpdateRequest request, @org.mapstruct.MappingTarget AiDocument document);
}
