// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiPromptCreateRequest;
import dev.strata.ai.api.request.AiPromptUpdateRequest;
import dev.strata.ai.config.api.dto.AiPromptDto;
import dev.strata.ai.config.biz.service.AiPromptAppService;
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
@RequestMapping("/api/v1/ai/prompts")
public class AiPromptController {

    private final AiPromptAppService aiPromptAppService;

    public AiPromptController(AiPromptAppService aiPromptAppService) {
        this.aiPromptAppService = aiPromptAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_PROMPT_LIST)
    public Result<List<AiPromptDto>> list() {
        return Result.ok(aiPromptAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_PROMPT_LIST)
    public Result<PageResult<AiPromptDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiPromptAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_PROMPT_LIST)
    public Result<AiPromptDto> get(@PathVariable Long id) {
        return Result.ok(aiPromptAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_PROMPT_CREATE)
    public Result<AiPromptDto> create(@Valid @RequestBody AiPromptCreateRequest request) {
        return Result.ok(aiPromptAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_PROMPT_UPDATE)
    public Result<AiPromptDto> update(@PathVariable Long id, @Valid @RequestBody AiPromptUpdateRequest request) {
        return Result.ok(aiPromptAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_PROMPT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiPromptAppService.delete(id);
        return Result.ok();
    }
}
