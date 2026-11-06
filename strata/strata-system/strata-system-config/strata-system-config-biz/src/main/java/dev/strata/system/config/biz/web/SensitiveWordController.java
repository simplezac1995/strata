// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.SensitiveWordDto;
import dev.strata.system.api.request.SensitiveWordCreateRequest;
import dev.strata.system.api.request.SensitiveWordUpdateRequest;
import dev.strata.system.config.biz.service.SensitiveWordAppService;
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
@RequestMapping("/api/v1/system/sensitive-words")
public class SensitiveWordController {

    private final SensitiveWordAppService sensitiveWordAppService;

    public SensitiveWordController(SensitiveWordAppService sensitiveWordAppService) {
        this.sensitiveWordAppService = sensitiveWordAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_SENSITIVEWORD_LIST)
    public Result<List<SensitiveWordDto>> list() {
        return Result.ok(sensitiveWordAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_SENSITIVEWORD_LIST)
    public Result<PageResult<SensitiveWordDto>> page(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(sensitiveWordAppService.page(category, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_SENSITIVEWORD_LIST)
    public Result<SensitiveWordDto> get(@PathVariable Long id) {
        return Result.ok(sensitiveWordAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_SENSITIVEWORD_CREATE)
    public Result<SensitiveWordDto> create(@Valid @RequestBody SensitiveWordCreateRequest request) {
        return Result.ok(sensitiveWordAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_SENSITIVEWORD_UPDATE)
    public Result<SensitiveWordDto> update(
            @PathVariable Long id, @Valid @RequestBody SensitiveWordUpdateRequest request) {
        return Result.ok(sensitiveWordAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_SENSITIVEWORD_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        sensitiveWordAppService.delete(id);
        return Result.ok();
    }
}
