// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.RoleDto;
import dev.strata.system.api.request.RoleCreateRequest;
import dev.strata.system.api.request.RoleUpdateRequest;
import dev.strata.system.rbac.api.entity.Role;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Role toEntity(RoleCreateRequest request);

    RoleDto toDto(Role role, List<String> menuIds);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void update(RoleUpdateRequest request, @MappingTarget Role role);
}
