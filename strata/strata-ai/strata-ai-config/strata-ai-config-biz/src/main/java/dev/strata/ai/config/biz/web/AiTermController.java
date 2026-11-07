// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.web;

import dev.strata.ai.api.request.AiTermCreateRequest;
import dev.strata.ai.api.request.AiTermUpdateRequest;
import dev.strata.ai.config.api.dto.AiTermDto;
import dev.strata.ai.config.biz.service.AiTermAppService;
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
@RequestMapping("/api/v1/ai/terms")
public class AiTermController {

    private final AiTermAppService aiTermAppService;

    public AiTermController(AiTermAppService aiTermAppService) {
        this.aiTermAppService = aiTermAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_TERM_LIST)
    public Result<List<AiTermDto>> list() {
        return Result.ok(aiTermAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.AI_TERM_LIST)
    public Result<PageResult<AiTermDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(aiTermAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.AI_TERM_LIST)
    public Result<AiTermDto> get(@PathVariable Long id) {
        return Result.ok(aiTermAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.AI_TERM_CREATE)
    public Result<AiTermDto> create(@Valid @RequestBody AiTermCreateRequest request) {
        return Result.ok(aiTermAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.AI_TERM_UPDATE)
    public Result<AiTermDto> update(@PathVariable Long id, @Valid @RequestBody AiTermUpdateRequest request) {
        return Result.ok(aiTermAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.AI_TERM_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        aiTermAppService.delete(id);
        return Result.ok();
    }
}
