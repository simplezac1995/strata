// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.web;

import dev.strata.ai.api.request.AiKnowledgeCreateRequest;
import dev.strata.ai.api.request.AiKnowledgeUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiKnowledgeDto;
import dev.strata.ai.knowledge.biz.service.AiKnowledgeAppService;
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
@RequestMapping("/api/v1/ai/knowledge-bases")
public class AiKnowledgeController {

    private final AiKnowledgeAppService aiKnowledgeAppService;

    public AiKnowledgeController(AiKnowledgeAppService aiKnowledgeAppService) {
        this.aiKnowledgeAppService = aiKnowledgeAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_KNOWLEDGE_LIST)
    public Result<List<AiKnowledgeDto>> list() {
        return Result.ok(aiKnowledgeAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_KNOWLEDGE_LIST)
    public Result<PageResult<AiKnowledgeDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiKnowledgeAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_KNOWLEDGE_LIST)
    public Result<AiKnowledgeDto> get(@PathVariable Long id) {
        return Result.ok(aiKnowledgeAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_KNOWLEDGE_CREATE)
    public Result<AiKnowledgeDto> create(@Valid @RequestBody AiKnowledgeCreateRequest request) {
        return Result.ok(aiKnowledgeAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_KNOWLEDGE_UPDATE)
    public Result<AiKnowledgeDto> update(@PathVariable Long id, @Valid @RequestBody AiKnowledgeUpdateRequest request) {
        return Result.ok(aiKnowledgeAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_KNOWLEDGE_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiKnowledgeAppService.delete(id);
        return Result.ok();
    }
}
