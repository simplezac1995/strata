// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.biz.repository.AiLlmUsageRepository;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** LLM call history (one row per gateway invocation). */
@RestController
@RequestMapping("/api/v1/ai/usage-logs")
public class AiUsageLogController {

    private final AiLlmUsageRepository repo;

    public AiUsageLogController(AiLlmUsageRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_BILLING_LIST)
    public Result<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var p = repo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var rows = p.getContent().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "provider", u.getProvider(),
                        "model", u.getModel(),
                        "action", u.getAction(),
                        "sessionId", u.getSessionId() == null ? "" : u.getSessionId(),
                        "promptTokens", u.getPromptTokens(),
                        "completionTokens", u.getCompletionTokens(),
                        "totalTokens", u.getTotalTokens(),
                        "createdAt",
                                u.getCreatedAt() == null ? "" : u.getCreatedAt().toString()))
                .toList();
        return Result.ok(new PageResult<>(rows, page, size, p.getTotalElements(), p.getTotalPages()));
    }
}
