// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.mapper;

import dev.strata.system.api.dto.AnnouncementDto;
import dev.strata.system.api.request.AnnouncementCreateRequest;
import dev.strata.system.api.request.AnnouncementUpdateRequest;
import dev.strata.system.communication.api.entity.Announcement;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnnouncementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", defaultValue = "true")
    Announcement toEntity(AnnouncementCreateRequest request);

    AnnouncementDto toDto(Announcement announcement);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(AnnouncementUpdateRequest request, @org.mapstruct.MappingTarget Announcement announcement);
}
