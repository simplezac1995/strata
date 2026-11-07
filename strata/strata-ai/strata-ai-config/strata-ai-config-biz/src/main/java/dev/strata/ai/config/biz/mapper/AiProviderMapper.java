// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiProviderCreateRequest;
import dev.strata.ai.api.request.AiProviderUpdateRequest;
import dev.strata.ai.config.api.dto.AiProviderDto;
import dev.strata.ai.config.api.entity.AiProvider;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiProviderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiProvider toEntity(AiProviderCreateRequest request);

    // ** Secrets never leave the server — masked in every response. */
    @Mapping(target = "apiKey", ignore = true)
    AiProviderDto toDto(AiProvider provider);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void update(AiProviderUpdateRequest request, @org.mapstruct.MappingTarget AiProvider provider);
}
