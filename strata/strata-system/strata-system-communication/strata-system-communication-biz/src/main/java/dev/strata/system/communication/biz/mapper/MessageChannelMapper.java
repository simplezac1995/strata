// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.mapper;

import dev.strata.system.api.dto.MessageChannelDto;
import dev.strata.system.communication.api.entity.MessageChannel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageChannelMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(channel.getId()))")
    MessageChannelDto toDto(MessageChannel channel);
}
