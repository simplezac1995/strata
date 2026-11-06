// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.web;

import dev.strata.ai.api.request.AiDatasetCreateRequest;
import dev.strata.ai.api.request.AiDatasetUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDatasetDto;
import dev.strata.ai.knowledge.biz.service.AiDatasetAppService;
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
@RequestMapping("/api/v1/ai/datasets")
public class AiDatasetController {

    private final AiDatasetAppService aiDatasetAppService;

    public AiDatasetController(AiDatasetAppService aiDatasetAppService) {
        this.aiDatasetAppService = aiDatasetAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_DATASET_LIST)
    public Result<List<AiDatasetDto>> list() {
        return Result.ok(aiDatasetAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_DATASET_LIST)
    public Result<PageResult<AiDatasetDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiDatasetAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_DATASET_LIST)
    public Result<AiDatasetDto> get(@PathVariable Long id) {
        return Result.ok(aiDatasetAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_DATASET_CREATE)
    public Result<AiDatasetDto> create(@Valid @RequestBody AiDatasetCreateRequest request) {
        return Result.ok(aiDatasetAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_DATASET_UPDATE)
    public Result<AiDatasetDto> update(@PathVariable Long id, @Valid @RequestBody AiDatasetUpdateRequest request) {
        return Result.ok(aiDatasetAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_DATASET_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiDatasetAppService.delete(id);
        return Result.ok();
    }
}
