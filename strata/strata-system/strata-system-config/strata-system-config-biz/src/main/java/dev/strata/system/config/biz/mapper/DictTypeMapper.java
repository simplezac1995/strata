// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.DictTypeDto;
import dev.strata.system.api.request.DictTypeCreateRequest;
import dev.strata.system.api.request.DictTypeUpdateRequest;
import dev.strata.system.config.api.entity.DictType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictTypeMapper {

    @Mapping(target = "id", ignore = true)
    DictType toEntity(DictTypeCreateRequest request);

    DictTypeDto toDto(DictType type);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void update(DictTypeUpdateRequest request, @org.mapstruct.MappingTarget DictType type);
}
