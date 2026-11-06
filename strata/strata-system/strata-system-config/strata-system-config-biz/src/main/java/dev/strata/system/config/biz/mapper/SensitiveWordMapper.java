// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.SensitiveWordDto;
import dev.strata.system.api.request.SensitiveWordCreateRequest;
import dev.strata.system.api.request.SensitiveWordUpdateRequest;
import dev.strata.system.config.api.entity.SensitiveWord;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SensitiveWordMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    SensitiveWord toEntity(SensitiveWordCreateRequest request);

    SensitiveWordDto toDto(SensitiveWord word);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(SensitiveWordUpdateRequest request, @org.mapstruct.MappingTarget SensitiveWord word);
}
