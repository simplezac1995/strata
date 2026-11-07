// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.web;

import dev.strata.ai.knowledge.api.dto.AiDocumentChunkDto;
import dev.strata.ai.knowledge.biz.service.AiDocumentChunkQueryService;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Browse document chunks (RAG observability). */
@RestController
@RequestMapping("/api/v1/ai/chunks")
public class AiChunkController {

    private final AiDocumentChunkQueryService chunks;

    public AiChunkController(AiDocumentChunkQueryService chunks) {
        this.chunks = chunks;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_KNOWLEDGE_LIST)
    public Result<PageResult<AiDocumentChunkDto>> page(
            @RequestParam(required = false) Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(chunks.page(documentId, page, size));
    }
}
