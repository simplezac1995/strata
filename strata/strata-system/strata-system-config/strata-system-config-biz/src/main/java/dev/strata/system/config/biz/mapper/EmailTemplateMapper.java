// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.EmailTemplateDto;
import dev.strata.system.api.request.EmailTemplateCreateRequest;
import dev.strata.system.api.request.EmailTemplateUpdateRequest;
import dev.strata.system.config.api.entity.EmailTemplate;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmailTemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    EmailTemplate toEntity(EmailTemplateCreateRequest request);

    EmailTemplateDto toDto(EmailTemplate template);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void update(EmailTemplateUpdateRequest request, @org.mapstruct.MappingTarget EmailTemplate template);
}
