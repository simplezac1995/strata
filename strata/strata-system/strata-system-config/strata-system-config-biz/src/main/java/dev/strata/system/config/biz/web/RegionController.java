// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.RegionDto;
import dev.strata.system.api.request.RegionCreateRequest;
import dev.strata.system.api.request.RegionUpdateRequest;
import dev.strata.system.config.biz.service.RegionAppService;
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
@RequestMapping("/api/v1/system/regions")
public class RegionController {

    private final RegionAppService regionAppService;

    public RegionController(RegionAppService regionAppService) {
        this.regionAppService = regionAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_REGION_LIST)
    public Result<List<RegionDto>> list() {
        return Result.ok(regionAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_REGION_LIST)
    public Result<PageResult<RegionDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(regionAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_REGION_LIST)
    public Result<RegionDto> get(@PathVariable Long id) {
        return Result.ok(regionAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_REGION_CREATE)
    public Result<RegionDto> create(@Valid @RequestBody RegionCreateRequest request) {
        return Result.ok(regionAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_REGION_UPDATE)
    public Result<RegionDto> update(@PathVariable Long id, @Valid @RequestBody RegionUpdateRequest request) {
        return Result.ok(regionAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_REGION_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        regionAppService.delete(id);
        return Result.ok();
    }
}
