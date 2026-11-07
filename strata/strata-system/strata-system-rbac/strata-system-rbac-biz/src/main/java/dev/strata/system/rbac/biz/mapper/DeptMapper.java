// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.DeptDto;
import dev.strata.system.api.request.DeptCreateRequest;
import dev.strata.system.api.request.DeptUpdateRequest;
import dev.strata.system.rbac.api.entity.Dept;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeptMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", source = "parentId", qualifiedByName = "blankToLong")
    Dept toEntity(DeptCreateRequest request);

    @Mapping(target = "children", ignore = true)
    DeptDto toDto(Dept dept);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", source = "parentId", qualifiedByName = "blankToLong")
    void update(DeptUpdateRequest request, @MappingTarget Dept dept);

    @Named("blankToLong")
    static Long blankToLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }
}
