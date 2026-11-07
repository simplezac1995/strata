// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.RegionDto;
import dev.strata.system.api.request.RegionCreateRequest;
import dev.strata.system.api.request.RegionUpdateRequest;
import dev.strata.system.config.api.entity.Region;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", source = "parentId", qualifiedByName = "blankToLong")
    Region toEntity(RegionCreateRequest request);

    RegionDto toDto(Region region);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "parentId", source = "parentId", qualifiedByName = "blankToLong")
    void update(RegionUpdateRequest request, @org.mapstruct.MappingTarget Region region);

    @Named("blankToLong")
    static Long blankToLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }
}
