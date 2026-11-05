// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.RiskIpBlacklistDto;
import dev.strata.system.api.request.RiskIpBlacklistCreateRequest;
import dev.strata.system.ops.api.entity.RiskIpBlacklist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskIpBlacklistMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RiskIpBlacklist toEntity(RiskIpBlacklistCreateRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    RiskIpBlacklistDto toDto(RiskIpBlacklist row);
}
