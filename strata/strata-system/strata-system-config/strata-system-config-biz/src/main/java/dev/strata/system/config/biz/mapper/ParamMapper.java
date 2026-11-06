// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.ParamDto;
import dev.strata.system.api.request.ParamCreateRequest;
import dev.strata.system.api.request.ParamUpdateRequest;
import dev.strata.system.config.api.entity.SysParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ParamMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", source = "type", defaultValue = "string")
    SysParam toEntity(ParamCreateRequest request);

    ParamDto toDto(SysParam param);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "key", ignore = true)
    void update(ParamUpdateRequest request, @org.mapstruct.MappingTarget SysParam param);
}
