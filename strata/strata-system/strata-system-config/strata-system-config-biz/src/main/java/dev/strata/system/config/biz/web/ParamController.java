// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.ParamDto;
import dev.strata.system.api.request.ParamCreateRequest;
import dev.strata.system.api.request.ParamUpdateRequest;
import dev.strata.system.config.biz.service.ParamAppService;
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
@RequestMapping("/api/v1/system/params")
public class ParamController {

    private final ParamAppService paramAppService;

    public ParamController(ParamAppService paramAppService) {
        this.paramAppService = paramAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_PARAM_LIST)
    public Result<List<ParamDto>> list() {
        return Result.ok(paramAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_PARAM_LIST)
    public Result<PageResult<ParamDto>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(paramAppService.page(keyword, page, size));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_PARAM_CREATE)
    public Result<ParamDto> create(@Valid @RequestBody ParamCreateRequest request) {
        return Result.ok(paramAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_PARAM_UPDATE)
    public Result<ParamDto> update(@PathVariable Long id, @Valid @RequestBody ParamUpdateRequest request) {
        return Result.ok(paramAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_PARAM_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        paramAppService.delete(id);
        return Result.ok();
    }
}
