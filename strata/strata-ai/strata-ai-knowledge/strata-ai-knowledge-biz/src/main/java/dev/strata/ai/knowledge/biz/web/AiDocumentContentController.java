// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.web;

import dev.strata.ai.api.request.AiDocumentContentRequest;
import dev.strata.ai.knowledge.biz.service.DocumentContentAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingests the plain-text body of a document under a knowledge base (the document row only stores metadata).
 * Sits under the {@code /api/v1/ai/knowledge-bases} tree so the frontend can POST
 * {@code /knowledge-bases/documents/{id}/content}.
 */
@Validated
@RestController
@RequestMapping("/api/v1/ai/knowledge-bases/documents")
public class AiDocumentContentController {

    private final DocumentContentAppService documentContentAppService;

    public AiDocumentContentController(DocumentContentAppService documentContentAppService) {
        this.documentContentAppService = documentContentAppService;
    }

    @PostMapping("/{id}/content")
    @PreAuthorize(Permissions.AI_DOCUMENT_UPDATE)
    public Result<Integer> attachContent(@PathVariable Long id, @Valid @RequestBody AiDocumentContentRequest request) {
        return Result.ok(documentContentAppService.attachContent(id, request.text()));
    }
}
