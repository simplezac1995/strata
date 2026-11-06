// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.MenuDto;
import dev.strata.system.api.request.MenuCreateRequest;
import dev.strata.system.api.request.MenuUpdateRequest;
import dev.strata.system.rbac.api.entity.Menu;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", source = "parentId", qualifiedByName = "blankToLong")
    Menu toEntity(MenuCreateRequest request);

    @Mapping(target = "children", ignore = true)
    MenuDto toDto(Menu menu);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", source = "parentId", qualifiedByName = "blankToLong")
    void update(MenuUpdateRequest request, @MappingTarget Menu menu);

    @Named("blankToLong")
    static Long blankToLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }
}
