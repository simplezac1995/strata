// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.api.dto.AiOfficeResponse;
import dev.strata.ai.api.request.AiOfficeRequest;
import dev.strata.ai.biz.service.AiOfficeAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AiOfficeController {

    private final AiOfficeAppService aiOfficeAppService;

    public AiOfficeController(AiOfficeAppService aiOfficeAppService) {
        this.aiOfficeAppService = aiOfficeAppService;
    }

    @PostMapping("/api/v1/ai/office/{action}")
    @PreAuthorize(Permissions.AI_CHAT_LIST)
    public Result<AiOfficeResponse> run(@PathVariable String action, @Valid @RequestBody AiOfficeRequest request) {
        return Result.ok(aiOfficeAppService.run(action, request));
    }
}
