// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.I18nEntryDto;
import dev.strata.system.api.request.I18nEntryCreateRequest;
import dev.strata.system.api.request.I18nEntryUpdateRequest;
import dev.strata.system.config.biz.service.I18nEntryAppService;
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
@RequestMapping("/api/v1/system/i18n")
public class I18nEntryController {

    private final I18nEntryAppService i18nEntryAppService;

    public I18nEntryController(I18nEntryAppService i18nEntryAppService) {
        this.i18nEntryAppService = i18nEntryAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_I18N_LIST)
    public Result<PageResult<I18nEntryDto>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(i18nEntryAppService.page(keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_I18N_LIST)
    public Result<I18nEntryDto> get(@PathVariable Long id) {
        return Result.ok(i18nEntryAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_I18N_CREATE)
    public Result<I18nEntryDto> create(@Valid @RequestBody I18nEntryCreateRequest request) {
        return Result.ok(i18nEntryAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_I18N_UPDATE)
    public Result<I18nEntryDto> update(@PathVariable Long id, @Valid @RequestBody I18nEntryUpdateRequest request) {
        return Result.ok(i18nEntryAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_I18N_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        i18nEntryAppService.delete(id);
        return Result.ok();
    }
}
