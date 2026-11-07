// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.api.dto.QaRetrieveResponse;
import dev.strata.ai.api.request.QaRetrieveRequest;
import dev.strata.ai.biz.service.AiQaAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AiQaController {

    private final AiQaAppService aiQaAppService;

    public AiQaController(AiQaAppService aiQaAppService) {
        this.aiQaAppService = aiQaAppService;
    }

    @PostMapping("/api/v1/ai/qa/retrieve")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<QaRetrieveResponse> retrieve(@Valid @RequestBody QaRetrieveRequest request) {
        return Result.ok(aiQaAppService.retrieve(request));
    }
}
