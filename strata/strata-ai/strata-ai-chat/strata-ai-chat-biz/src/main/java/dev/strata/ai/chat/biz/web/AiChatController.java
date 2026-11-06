// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.web;

import dev.strata.ai.api.request.AiChatMessageCreateRequest;
import dev.strata.ai.api.request.AiChatSessionCreateRequest;
import dev.strata.ai.chat.api.dto.AiChatMessageDto;
import dev.strata.ai.chat.api.dto.AiChatSessionDto;
import dev.strata.ai.chat.biz.service.AiChatAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api/v1/ai/chat/sessions")
public class AiChatController {

    private final AiChatAppService aiChatAppService;

    public AiChatController(AiChatAppService aiChatAppService) {
        this.aiChatAppService = aiChatAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<List<AiChatSessionDto>> listSessions() {
        return Result.ok(aiChatAppService.listSessions());
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_CHAT_CREATE)
    public Result<AiChatSessionDto> createSession(@Valid @RequestBody AiChatSessionCreateRequest request) {
        return Result.ok(aiChatAppService.createSession(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_CHAT_CREATE)
    public Result<AiChatSessionDto> renameSession(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(aiChatAppService.renameSession(id, body.get("title")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_CHAT_DELETE)
    public Result<Void> deleteSession(@PathVariable Long id) {
        aiChatAppService.deleteSession(id);
        return Result.ok();
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<List<AiChatMessageDto>> listMessages(@PathVariable Long id) {
        return Result.ok(aiChatAppService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize(Permissions.AI_CHAT_CREATE)
    public Result<AiChatMessageDto> sendMessage(
            @PathVariable Long id, @Valid @RequestBody AiChatMessageCreateRequest request) {
        return Result.ok(aiChatAppService.sendMessage(id, request));
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(Permissions.AI_CHAT_CREATE)
    public SseEmitter sendMessageStream(@PathVariable Long id, @Valid @RequestBody AiChatMessageCreateRequest request) {
        return aiChatAppService.sendMessageStream(id, request);
    }
}
