// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.TenantDto;
import dev.strata.system.api.request.TenantCreateRequest;
import dev.strata.system.api.request.TenantUpdateRequest;
import dev.strata.system.rbac.api.entity.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    @Mapping(target = "id", ignore = true)
    Tenant toEntity(TenantCreateRequest request);

    TenantDto toDto(Tenant tenant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void update(TenantUpdateRequest request, @org.mapstruct.MappingTarget Tenant tenant);
}
