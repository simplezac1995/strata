// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.ApiKeyCreatedDto;
import dev.strata.system.api.dto.ApiKeyDto;
import dev.strata.system.api.dto.ApiKeySummaryDto;
import dev.strata.system.api.request.ApiKeyCreateRequest;
import dev.strata.system.api.request.ApiKeyUpdateRequest;
import dev.strata.system.rbac.biz.service.ApiKeyAppService;
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
@RequestMapping("/api/v1/system/api-keys")
public class ApiKeyController {

    private final ApiKeyAppService apiKeyAppService;

    public ApiKeyController(ApiKeyAppService apiKeyAppService) {
        this.apiKeyAppService = apiKeyAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_APIKEY_LIST)
    public Result<List<ApiKeyDto>> list() {
        return Result.ok(apiKeyAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_APIKEY_LIST)
    public Result<PageResult<ApiKeyDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(apiKeyAppService.page(page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize(Permissions.SYSTEM_APIKEY_LIST)
    public Result<ApiKeySummaryDto> summary() {
        return Result.ok(apiKeyAppService.summary());
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_APIKEY_LIST)
    public Result<ApiKeyDto> get(@PathVariable Long id) {
        return Result.ok(apiKeyAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_APIKEY_CREATE)
    public Result<ApiKeyCreatedDto> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return Result.ok(apiKeyAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_APIKEY_UPDATE)
    public Result<ApiKeyDto> update(@PathVariable Long id, @Valid @RequestBody ApiKeyUpdateRequest request) {
        return Result.ok(apiKeyAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_APIKEY_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        apiKeyAppService.delete(id);
        return Result.ok();
    }
}
