// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.BlockedRegionDto;
import dev.strata.system.api.request.compliance.BlockedRegionRequest;
import dev.strata.system.ops.api.entity.compliance.BlockedRegion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BlockedRegionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    BlockedRegion toEntity(BlockedRegionRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    BlockedRegionDto toDto(BlockedRegion row);
}
