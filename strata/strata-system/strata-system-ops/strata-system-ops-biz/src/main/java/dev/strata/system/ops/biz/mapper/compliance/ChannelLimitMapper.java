// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper.compliance;

import dev.strata.system.api.dto.compliance.ChannelLimitDto;
import dev.strata.system.api.request.compliance.ChannelLimitCreateRequest;
import dev.strata.system.api.request.compliance.ChannelLimitUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.ChannelLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChannelLimitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ChannelLimit toEntity(ChannelLimitCreateRequest request);

    @Mapping(target = "id", expression = "java(String.valueOf(row.getId()))")
    ChannelLimitDto toDto(ChannelLimit row);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(ChannelLimitUpdateRequest request, @MappingTarget ChannelLimit row);
}
