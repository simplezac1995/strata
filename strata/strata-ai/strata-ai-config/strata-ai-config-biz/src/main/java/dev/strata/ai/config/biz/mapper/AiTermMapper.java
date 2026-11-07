// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiTermCreateRequest;
import dev.strata.ai.api.request.AiTermUpdateRequest;
import dev.strata.ai.config.api.dto.AiTermDto;
import dev.strata.ai.config.api.entity.AiTerm;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiTermMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiTerm toEntity(AiTermCreateRequest request);

    AiTermDto toDto(AiTerm term);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "term", ignore = true)
    void update(AiTermUpdateRequest request, @org.mapstruct.MappingTarget AiTerm term);
}
