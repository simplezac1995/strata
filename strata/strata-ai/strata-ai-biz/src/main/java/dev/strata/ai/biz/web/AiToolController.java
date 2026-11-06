// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.ai.api.dto.AiToolRunDto;
import dev.strata.ai.api.dto.AiUsageDto;
import dev.strata.ai.api.request.AiToolRunRequest;
import dev.strata.ai.biz.service.AiToolAppService;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ai/tools")
public class AiToolController {

    private final AiToolAppService aiToolAppService;

    public AiToolController(AiToolAppService aiToolAppService) {
        this.aiToolAppService = aiToolAppService;
    }

    @GetMapping("/usage")
    @PreAuthorize(Permissions.AI_TOOL_LIST)
    public Result<AiUsageDto> usage() {
        return Result.ok(aiToolAppService.usage());
    }

    @PostMapping("/run")
    @PreAuthorize(Permissions.AI_TOOL_CREATE)
    public Result<AiToolRunDto> run(@Valid @RequestBody AiToolRunRequest request) {
        return Result.ok(aiToolAppService.run(request));
    }

    @GetMapping("/video/status/{taskId}")
    @PreAuthorize(Permissions.AI_TOOL_LIST)
    public Result<java.util.Map<String, Object>> videoStatus(@PathVariable String taskId) {
        return Result.ok(aiToolAppService.videoTaskStatus(taskId));
    }
}
