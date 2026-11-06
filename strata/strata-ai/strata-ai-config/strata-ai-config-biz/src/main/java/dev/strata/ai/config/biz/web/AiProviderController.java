// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiProviderCreateRequest;
import dev.strata.ai.api.request.AiProviderUpdateRequest;
import dev.strata.ai.config.api.dto.AiProviderDto;
import dev.strata.ai.config.biz.service.AiProviderAppService;
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
@RequestMapping("/api/v1/ai/providers")
public class AiProviderController {

    private final AiProviderAppService aiProviderAppService;

    public AiProviderController(AiProviderAppService aiProviderAppService) {
        this.aiProviderAppService = aiProviderAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_PROVIDER_LIST)
    public Result<List<AiProviderDto>> list() {
        return Result.ok(aiProviderAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_PROVIDER_LIST)
    public Result<PageResult<AiProviderDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiProviderAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_PROVIDER_LIST)
    public Result<AiProviderDto> get(@PathVariable Long id) {
        return Result.ok(aiProviderAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_PROVIDER_CREATE)
    public Result<AiProviderDto> create(@Valid @RequestBody AiProviderCreateRequest request) {
        return Result.ok(aiProviderAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_PROVIDER_UPDATE)
    public Result<AiProviderDto> update(@PathVariable Long id, @Valid @RequestBody AiProviderUpdateRequest request) {
        return Result.ok(aiProviderAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_PROVIDER_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiProviderAppService.delete(id);
        return Result.ok();
    }
}
