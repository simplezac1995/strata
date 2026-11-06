// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.LoginLogDto;
import dev.strata.system.api.event.LoginLogEvent;
import dev.strata.system.ops.api.entity.LoginLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoginLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    LoginLog toEntity(LoginLogEvent event);

    LoginLogDto toDto(LoginLog log);
}
