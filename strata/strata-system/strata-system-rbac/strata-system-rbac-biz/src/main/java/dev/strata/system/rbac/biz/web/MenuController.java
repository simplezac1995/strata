// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.MenuDto;
import dev.strata.system.api.request.MenuCreateRequest;
import dev.strata.system.api.request.MenuUpdateRequest;
import dev.strata.system.rbac.biz.service.MenuAppService;
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
@RequestMapping("/api/v1/system/menus")
public class MenuController {

    private final MenuAppService menuAppService;

    public MenuController(MenuAppService menuAppService) {
        this.menuAppService = menuAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_MENU_LIST)
    public Result<List<MenuDto>> list() {
        return Result.ok(menuAppService.list());
    }

    @GetMapping("/children")
    @PreAuthorize(Permissions.SYSTEM_MENU_LIST)
    public Result<List<MenuDto>> children(@RequestParam(required = false) Long parentId) {
        return Result.ok(menuAppService.listChildren(parentId));
    }

    @GetMapping("/flat")
    @PreAuthorize(Permissions.SYSTEM_MENU_LIST)
    public Result<PageResult<MenuDto>> pageFlat(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(menuAppService.pageFlat(keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_MENU_LIST)
    public Result<MenuDto> get(@PathVariable Long id) {
        return Result.ok(menuAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_MENU_CREATE)
    public Result<MenuDto> create(@Valid @RequestBody MenuCreateRequest request) {
        return Result.ok(menuAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_MENU_UPDATE)
    public Result<MenuDto> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        return Result.ok(menuAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_MENU_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        menuAppService.delete(id);
        return Result.ok();
    }
}
