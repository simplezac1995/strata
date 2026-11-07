// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.ApiKeyDto;
import dev.strata.system.rbac.api.entity.ApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApiKeyMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(key.getId()))")
    ApiKeyDto toDto(ApiKey key);
}
