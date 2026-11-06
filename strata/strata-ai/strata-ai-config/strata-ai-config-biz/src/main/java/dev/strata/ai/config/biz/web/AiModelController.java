// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiModelCreateRequest;
import dev.strata.ai.api.request.AiModelUpdateRequest;
import dev.strata.ai.config.api.dto.AiModelDto;
import dev.strata.ai.config.biz.service.AiModelAppService;
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
@RequestMapping("/api/v1/ai/models")
public class AiModelController {

    private final AiModelAppService aiModelAppService;

    public AiModelController(AiModelAppService aiModelAppService) {
        this.aiModelAppService = aiModelAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_MODEL_LIST)
    public Result<List<AiModelDto>> list() {
        return Result.ok(aiModelAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_MODEL_LIST)
    public Result<PageResult<AiModelDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiModelAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_MODEL_LIST)
    public Result<AiModelDto> get(@PathVariable Long id) {
        return Result.ok(aiModelAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_MODEL_CREATE)
    public Result<AiModelDto> create(@Valid @RequestBody AiModelCreateRequest request) {
        return Result.ok(aiModelAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_MODEL_UPDATE)
    public Result<AiModelDto> update(@PathVariable Long id, @Valid @RequestBody AiModelUpdateRequest request) {
        return Result.ok(aiModelAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_MODEL_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiModelAppService.delete(id);
        return Result.ok();
    }
}
