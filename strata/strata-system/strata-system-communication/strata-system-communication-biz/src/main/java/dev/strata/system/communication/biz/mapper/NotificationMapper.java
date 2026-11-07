// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.mapper;

import dev.strata.system.api.dto.NotificationDto;
import dev.strata.system.communication.api.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);
}
