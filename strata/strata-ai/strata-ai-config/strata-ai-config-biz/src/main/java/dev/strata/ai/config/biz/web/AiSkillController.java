// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiSkillCreateRequest;
import dev.strata.ai.api.request.AiSkillUpdateRequest;
import dev.strata.ai.config.api.dto.AiSkillDto;
import dev.strata.ai.config.biz.service.AiSkillAppService;
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
@RequestMapping("/api/v1/ai/skills")
public class AiSkillController {

    private final AiSkillAppService aiSkillAppService;

    public AiSkillController(AiSkillAppService aiSkillAppService) {
        this.aiSkillAppService = aiSkillAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_SKILL_LIST)
    public Result<List<AiSkillDto>> list() {
        return Result.ok(aiSkillAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_SKILL_LIST)
    public Result<PageResult<AiSkillDto>> page(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiSkillAppService.page(type, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_SKILL_LIST)
    public Result<AiSkillDto> get(@PathVariable Long id) {
        return Result.ok(aiSkillAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_SKILL_CREATE)
    public Result<AiSkillDto> create(@Valid @RequestBody AiSkillCreateRequest request) {
        return Result.ok(aiSkillAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_SKILL_UPDATE)
    public Result<AiSkillDto> update(@PathVariable Long id, @Valid @RequestBody AiSkillUpdateRequest request) {
        return Result.ok(aiSkillAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_SKILL_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiSkillAppService.delete(id);
        return Result.ok();
    }
}
