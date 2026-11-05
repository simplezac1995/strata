// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.OauthClientDto;
import dev.strata.system.api.request.OauthClientCreateRequest;
import dev.strata.system.api.request.OauthClientUpdateRequest;
import dev.strata.system.rbac.biz.service.OauthClientAppService;
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
@RequestMapping("/api/v1/system/clients")
public class OauthClientController {

    private final OauthClientAppService oauthClientAppService;

    public OauthClientController(OauthClientAppService oauthClientAppService) {
        this.oauthClientAppService = oauthClientAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_CLIENT_LIST)
    public Result<List<OauthClientDto>> list() {
        return Result.ok(oauthClientAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_CLIENT_LIST)
    public Result<PageResult<OauthClientDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(oauthClientAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_CLIENT_LIST)
    public Result<OauthClientDto> get(@PathVariable Long id) {
        return Result.ok(oauthClientAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_CLIENT_CREATE)
    public Result<OauthClientDto> create(@Valid @RequestBody OauthClientCreateRequest request) {
        return Result.ok(oauthClientAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_CLIENT_UPDATE)
    public Result<OauthClientDto> update(@PathVariable Long id, @Valid @RequestBody OauthClientUpdateRequest request) {
        return Result.ok(oauthClientAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_CLIENT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        oauthClientAppService.delete(id);
        return Result.ok();
    }
}
