// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.PopupConfigDto;
import dev.strata.system.api.request.PopupUpsertRequest;
import dev.strata.system.ops.api.entity.PopupConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PopupConfigMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", source = "enabled", defaultValue = "true")
    PopupConfig toEntity(PopupUpsertRequest request);

    PopupConfigDto toDto(PopupConfig popupConfig);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(PopupUpsertRequest request, @org.mapstruct.MappingTarget PopupConfig popupConfig);
}
