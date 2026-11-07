// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiAgentCreateRequest;
import dev.strata.ai.api.request.AiAgentUpdateRequest;
import dev.strata.ai.config.api.dto.AiAgentDto;
import dev.strata.ai.config.biz.service.AiAgentAppService;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ai/agents")
public class AiAgentController {

    private final AiAgentAppService aiAgentAppService;

    public AiAgentController(AiAgentAppService aiAgentAppService) {
        this.aiAgentAppService = aiAgentAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_AGENT_LIST)
    public Result<List<AiAgentDto>> list() {
        return Result.ok(aiAgentAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_AGENT_LIST)
    public Result<PageResult<AiAgentDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiAgentAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_AGENT_LIST)
    public Result<AiAgentDto> get(@PathVariable Long id) {
        return Result.ok(aiAgentAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_AGENT_CREATE)
    public Result<AiAgentDto> create(@Valid @RequestBody AiAgentCreateRequest request) {
        return Result.ok(aiAgentAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_AGENT_UPDATE)
    public Result<AiAgentDto> update(@PathVariable Long id, @Valid @RequestBody AiAgentUpdateRequest request) {
        return Result.ok(aiAgentAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_AGENT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiAgentAppService.delete(id);
        return Result.ok();
    }
}
