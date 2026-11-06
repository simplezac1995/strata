// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.GeoRestrictionDto;
import dev.strata.system.api.request.compliance.GeoRestrictionCreateRequest;
import dev.strata.system.api.request.compliance.GeoRestrictionUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.GeoRestriction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GeoRestrictionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    GeoRestriction toEntity(GeoRestrictionCreateRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    GeoRestrictionDto toDto(GeoRestriction row);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(GeoRestrictionUpdateRequest request, @MappingTarget GeoRestriction row);
}
