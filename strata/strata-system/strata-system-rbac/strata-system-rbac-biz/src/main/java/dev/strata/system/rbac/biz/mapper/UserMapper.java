// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.UserCreateRequest;
import dev.strata.system.api.request.UserUpdateRequest;
import dev.strata.system.rbac.api.entity.User;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    User toEntity(UserCreateRequest request);

    @Mapping(target = "mfaEnabled", source = "user.totpEnabled")
    UserDto toDto(User user, List<String> roleIds);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void update(UserUpdateRequest request, @MappingTarget User user);
}
