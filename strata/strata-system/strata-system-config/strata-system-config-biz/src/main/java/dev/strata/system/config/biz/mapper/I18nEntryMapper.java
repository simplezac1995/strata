// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.I18nEntryDto;
import dev.strata.system.api.request.I18nEntryCreateRequest;
import dev.strata.system.api.request.I18nEntryUpdateRequest;
import dev.strata.system.config.api.entity.I18nEntry;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface I18nEntryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    I18nEntry toEntity(I18nEntryCreateRequest request);

    @Mapping(
            target = "updatedBy",
            expression = "java(entry.getUpdatedBy() == null ? null : String.valueOf(entry.getUpdatedBy()))")
    I18nEntryDto toDto(I18nEntry entry);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "messageKey", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void update(I18nEntryUpdateRequest request, @org.mapstruct.MappingTarget I18nEntry entry);
}
