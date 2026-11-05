// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.web;

import dev.strata.ai.api.request.AiDocumentCreateRequest;
import dev.strata.ai.api.request.AiDocumentUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDocumentDto;
import dev.strata.ai.knowledge.biz.service.AiDocumentAppService;
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
@RequestMapping("/api/v1/ai/documents")
public class AiDocumentController {

    private final AiDocumentAppService aiDocumentAppService;

    public AiDocumentController(AiDocumentAppService aiDocumentAppService) {
        this.aiDocumentAppService = aiDocumentAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_DOCUMENT_LIST)
    public Result<List<AiDocumentDto>> list(@RequestParam(required = false) Long knowledgeId) {
        if (knowledgeId == null) {
            return Result.ok(aiDocumentAppService.listAll());
        }
        return Result.ok(aiDocumentAppService.listByKnowledge(knowledgeId));
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_DOCUMENT_LIST)
    public Result<PageResult<AiDocumentDto>> page(
            @RequestParam(required = false) Long knowledgeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiDocumentAppService.page(knowledgeId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_DOCUMENT_LIST)
    public Result<AiDocumentDto> get(@PathVariable Long id) {
        return Result.ok(aiDocumentAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_DOCUMENT_CREATE)
    public Result<AiDocumentDto> create(@Valid @RequestBody AiDocumentCreateRequest request) {
        return Result.ok(aiDocumentAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_DOCUMENT_UPDATE)
    public Result<AiDocumentDto> update(@PathVariable Long id, @Valid @RequestBody AiDocumentUpdateRequest request) {
        return Result.ok(aiDocumentAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_DOCUMENT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiDocumentAppService.delete(id);
        return Result.ok();
    }
}
