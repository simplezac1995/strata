// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.mapper;

import dev.strata.system.api.dto.SiteSettingDto;
import dev.strata.system.config.api.entity.SiteSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SiteSettingMapper {

    @Mapping(target = "key", source = "settingKey")
    @Mapping(target = "value", source = "settingValue")
    SiteSettingDto toDto(SiteSetting setting);
}
