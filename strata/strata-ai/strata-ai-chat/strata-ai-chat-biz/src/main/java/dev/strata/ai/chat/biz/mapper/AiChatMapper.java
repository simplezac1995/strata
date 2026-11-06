// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.mapper;

import dev.strata.ai.chat.api.dto.AiChatMessageDto;
import dev.strata.ai.chat.api.dto.AiChatSessionDto;
import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.chat.api.entity.AiChatSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiChatMapper {

    @Mapping(target = "id", expression = "java(String.valueOf(session.getId()))")
    @Mapping(
            target = "modelId",
            expression = "java(session.getModelId() == null ? null : String.valueOf(session.getModelId()))")
    @Mapping(target = "userId", expression = "java(String.valueOf(session.getUserId()))")
    AiChatSessionDto toSessionDto(AiChatSession session);

    @Mapping(target = "id", expression = "java(String.valueOf(message.getId()))")
    @Mapping(target = "sessionId", expression = "java(String.valueOf(message.getSessionId()))")
    AiChatMessageDto toMessageDto(AiChatMessage message);
}
