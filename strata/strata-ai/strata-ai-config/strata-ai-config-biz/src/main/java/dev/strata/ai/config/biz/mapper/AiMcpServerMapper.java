// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.mapper;

import dev.strata.ai.api.request.AiMcpServerCreateRequest;
import dev.strata.ai.api.request.AiMcpServerUpdateRequest;
import dev.strata.ai.config.api.dto.AiMcpServerDto;
import dev.strata.ai.config.api.entity.AiMcpServer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiMcpServerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    AiMcpServer toEntity(AiMcpServerCreateRequest request);

    // ** Secrets never leave the server — masked in every response. */
    @Mapping(target = "authToken", ignore = true)
    AiMcpServerDto toDto(AiMcpServer server);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(AiMcpServerUpdateRequest request, @org.mapstruct.MappingTarget AiMcpServer server);
}
