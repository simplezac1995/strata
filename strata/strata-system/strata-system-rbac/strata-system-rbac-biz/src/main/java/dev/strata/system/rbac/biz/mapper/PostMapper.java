// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import dev.strata.system.api.dto.PostDto;
import dev.strata.system.api.request.PostCreateRequest;
import dev.strata.system.api.request.PostUpdateRequest;
import dev.strata.system.rbac.api.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {

    @Mapping(target = "id", ignore = true)
    Post toEntity(PostCreateRequest request);

    PostDto toDto(Post post);

    @Mapping(target = "id", ignore = true)
    void update(PostUpdateRequest request, @org.mapstruct.MappingTarget Post post);
}
