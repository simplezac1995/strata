// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.mapper;

import dev.strata.system.api.dto.NotificationPrefDto;
import dev.strata.system.communication.api.entity.UserNotificationPref;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationPrefMapper {

    NotificationPrefDto toDto(UserNotificationPref pref);
}
