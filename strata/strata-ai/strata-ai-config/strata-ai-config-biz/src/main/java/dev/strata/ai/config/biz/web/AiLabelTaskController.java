// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiLabelSampleCreateRequest;
import dev.strata.ai.api.request.AiLabelTaskCreateRequest;
import dev.strata.ai.api.request.AiLabelTaskUpdateRequest;
import dev.strata.ai.config.api.dto.AiLabelSampleDto;
import dev.strata.ai.config.api.dto.AiLabelTaskDto;
import dev.strata.ai.config.biz.service.AiLabelTaskAppService;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/ai/labeling/tasks")
public class AiLabelTaskController {

    private final AiLabelTaskAppService aiLabelTaskAppService;

    public AiLabelTaskController(AiLabelTaskAppService aiLabelTaskAppService) {
        this.aiLabelTaskAppService = aiLabelTaskAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_LABEL_LIST)
    public Result<PageResult<AiLabelTaskDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiLabelTaskAppService.pageTasks(page, size));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_LABEL_CREATE)
    public Result<AiLabelTaskDto> create(@Valid @RequestBody AiLabelTaskCreateRequest request) {
        return Result.ok(aiLabelTaskAppService.createTask(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_LABEL_UPDATE)
    public Result<AiLabelTaskDto> update(@PathVariable Long id, @Valid @RequestBody AiLabelTaskUpdateRequest request) {
        return Result.ok(aiLabelTaskAppService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_LABEL_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiLabelTaskAppService.deleteTask(id);
        return Result.ok();
    }

    @GetMapping("/{id}/samples")
    @PreAuthorize(Permissions.AI_LABEL_LIST)
    public Result<PageResult<AiLabelSampleDto>> samples(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiLabelTaskAppService.pageSamples(id, page, size));
    }

    @PostMapping("/{id}/samples")
    @PreAuthorize(Permissions.AI_LABEL_CREATE)
    public Result<AiLabelSampleDto> addSample(
            @PathVariable Long id, @Valid @RequestBody AiLabelSampleCreateRequest request) {
        return Result.ok(aiLabelTaskAppService.addSample(id, request));
    }
}
