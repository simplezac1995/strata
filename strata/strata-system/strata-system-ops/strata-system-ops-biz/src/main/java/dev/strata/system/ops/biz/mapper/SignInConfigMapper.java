// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.SignInConfigDto;
import dev.strata.system.api.request.SignInConfigUpsertRequest.Item;
import dev.strata.system.ops.api.entity.SignInConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SignInConfigMapper {

    @Mapping(target = "id", ignore = true)
    SignInConfig toEntity(Item item);

    SignInConfigDto toDto(SignInConfig config);
}
