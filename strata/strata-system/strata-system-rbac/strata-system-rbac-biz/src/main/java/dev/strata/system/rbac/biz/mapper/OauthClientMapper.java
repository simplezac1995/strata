// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.OauthClientDto;
import dev.strata.system.api.request.OauthClientCreateRequest;
import dev.strata.system.api.request.OauthClientUpdateRequest;
import dev.strata.system.rbac.api.entity.OauthClient;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OauthClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    OauthClient toEntity(OauthClientCreateRequest request);

    // ** Secrets never leave the server 鈥?masked in every response. */
    @Mapping(target = "clientSecret", ignore = true)
    OauthClientDto toDto(OauthClient client);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "clientSecret", ignore = true)
    void update(OauthClientUpdateRequest request, @org.mapstruct.MappingTarget OauthClient client);
}
