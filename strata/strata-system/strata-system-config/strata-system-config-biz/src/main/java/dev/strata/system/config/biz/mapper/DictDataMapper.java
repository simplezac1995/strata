// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.DictDataDto;
import dev.strata.system.api.request.DictDataCreateRequest;
import dev.strata.system.api.request.DictDataUpdateRequest;
import dev.strata.system.config.api.entity.DictData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictDataMapper {

    @Mapping(target = "id", ignore = true)
    DictData toEntity(DictDataCreateRequest request);

    DictDataDto toDto(DictData data);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeId", ignore = true)
    @Mapping(target = "value", ignore = true)
    void update(DictDataUpdateRequest request, @org.mapstruct.MappingTarget DictData data);
}
