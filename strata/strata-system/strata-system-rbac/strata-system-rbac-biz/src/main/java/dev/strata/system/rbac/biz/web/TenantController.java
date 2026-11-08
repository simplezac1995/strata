// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.TenantDto;
import dev.strata.system.api.request.TenantCreateRequest;
import dev.strata.system.api.request.TenantUpdateRequest;
import dev.strata.system.rbac.biz.service.TenantAppService;
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
@RequestMapping("/api/v1/system/tenants")
public class TenantController {

    private final TenantAppService tenantAppService;

    public TenantController(TenantAppService tenantAppService) {
        this.tenantAppService = tenantAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_TENANT_LIST)
    public Result<List<TenantDto>> list() {
        return Result.ok(tenantAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_TENANT_LIST)
    public Result<PageResult<TenantDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(tenantAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_TENANT_LIST)
    public Result<TenantDto> get(@PathVariable Long id) {
        return Result.ok(tenantAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_TENANT_CREATE)
    public Result<TenantDto> create(@Valid @RequestBody TenantCreateRequest request) {
        return Result.ok(tenantAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_TENANT_UPDATE)
    public Result<TenantDto> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateRequest request) {
        return Result.ok(tenantAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_TENANT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        tenantAppService.delete(id);
        return Result.ok();
    }
}
