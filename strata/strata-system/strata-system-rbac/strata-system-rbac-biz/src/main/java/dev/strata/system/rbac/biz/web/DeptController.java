// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.DeptDto;
import dev.strata.system.api.request.DeptCreateRequest;
import dev.strata.system.api.request.DeptUpdateRequest;
import dev.strata.system.rbac.biz.service.DeptAppService;
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
@RequestMapping("/api/v1/system/depts")
public class DeptController {

    private final DeptAppService deptAppService;

    public DeptController(DeptAppService deptAppService) {
        this.deptAppService = deptAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_DEPT_LIST)
    public Result<List<DeptDto>> list() {
        return Result.ok(deptAppService.list());
    }

    @GetMapping("/children")
    @PreAuthorize(Permissions.SYSTEM_DEPT_LIST)
    public Result<List<DeptDto>> children(@RequestParam(required = false) Long parentId) {
        return Result.ok(deptAppService.listChildren(parentId));
    }

    @GetMapping("/flat")
    @PreAuthorize(Permissions.SYSTEM_DEPT_LIST)
    public Result<PageResult<DeptDto>> pageFlat(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(deptAppService.pageFlat(keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_DEPT_LIST)
    public Result<DeptDto> get(@PathVariable Long id) {
        return Result.ok(deptAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_DEPT_CREATE)
    public Result<DeptDto> create(@Valid @RequestBody DeptCreateRequest request) {
        return Result.ok(deptAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_DEPT_UPDATE)
    public Result<DeptDto> update(@PathVariable Long id, @Valid @RequestBody DeptUpdateRequest request) {
        return Result.ok(deptAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_DEPT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        deptAppService.delete(id);
        return Result.ok();
    }
}
