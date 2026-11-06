// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.RoleDto;
import dev.strata.system.api.request.RoleCreateRequest;
import dev.strata.system.api.request.RoleUpdateRequest;
import dev.strata.system.rbac.biz.service.RoleAppService;
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
@RequestMapping("/api/v1/system/roles")
public class RoleController {

    private final RoleAppService roleAppService;

    public RoleController(RoleAppService roleAppService) {
        this.roleAppService = roleAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_ROLE_LIST)
    public Result<List<RoleDto>> list() {
        return Result.ok(roleAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_ROLE_LIST)
    public Result<PageResult<RoleDto>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return Result.ok(roleAppService.page(keyword, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_ROLE_LIST)
    public Result<RoleDto> get(@PathVariable Long id) {
        return Result.ok(roleAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_ROLE_CREATE)
    public Result<RoleDto> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.ok(roleAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_ROLE_UPDATE)
    public Result<RoleDto> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return Result.ok(roleAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_ROLE_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        roleAppService.delete(id);
        return Result.ok();
    }
}
