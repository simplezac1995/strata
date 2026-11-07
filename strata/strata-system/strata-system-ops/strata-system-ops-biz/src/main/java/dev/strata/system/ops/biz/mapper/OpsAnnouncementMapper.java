// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import dev.strata.system.api.dto.OpsAnnouncementDto;
import dev.strata.system.api.request.OpsAnnouncementUpsertRequest;
import dev.strata.system.ops.api.entity.Announcement;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OpsAnnouncementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sort", source = "sort", defaultValue = "0")
    Announcement toEntity(OpsAnnouncementUpsertRequest request);

    OpsAnnouncementDto toDto(Announcement announcement);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(OpsAnnouncementUpsertRequest request, @org.mapstruct.MappingTarget Announcement announcement);
}
