// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.compliance.BlockedRegionDto;
import dev.strata.system.api.dto.compliance.GeoCheckResult;
import dev.strata.system.api.dto.compliance.GeoRestrictionDto;
import dev.strata.system.api.request.compliance.BlockedRegionRequest;
import dev.strata.system.api.request.compliance.GeoCheckIpRequest;
import dev.strata.system.api.request.compliance.GeoRestrictionCreateRequest;
import dev.strata.system.api.request.compliance.GeoRestrictionUpdateRequest;
import dev.strata.system.ops.biz.service.compliance.GeoAppService;
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

/** Admin geo-fencing: IP ranges, blocked countries and ad-hoc IP checks. */
@Validated
@RestController
@RequestMapping("/api/v1/admin/geo")
public class GeoAdminController {

    private final GeoAppService geoAppService;

    public GeoAdminController(GeoAppService geoAppService) {
        this.geoAppService = geoAppService;
    }

    @GetMapping("/restrictions")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_LIST)
    public Result<PageResult<GeoRestrictionDto>> pageRestrictions(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(geoAppService.pageRestrictions(countryCode, enabled, p, s));
    }

    @PostMapping("/restrictions")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_CREATE)
    public Result<GeoRestrictionDto> createRestriction(@Valid @RequestBody GeoRestrictionCreateRequest request) {
        return Result.ok(geoAppService.createRestriction(request));
    }

    @PutMapping("/restrictions/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_UPDATE)
    public Result<GeoRestrictionDto> updateRestriction(
            @PathVariable Long id, @Valid @RequestBody GeoRestrictionUpdateRequest request) {
        return Result.ok(geoAppService.updateRestriction(id, request));
    }

    @DeleteMapping("/restrictions/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_DELETE)
    public Result<Void> deleteRestriction(@PathVariable Long id) {
        geoAppService.deleteRestriction(id);
        return Result.ok();
    }

    @GetMapping("/regions")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_LIST)
    public Result<List<BlockedRegionDto>> listRegions() {
        return Result.ok(geoAppService.listRegions());
    }

    @PostMapping("/regions")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_CREATE)
    public Result<BlockedRegionDto> createRegion(@Valid @RequestBody BlockedRegionRequest request) {
        return Result.ok(geoAppService.createRegion(request));
    }

    @DeleteMapping("/regions/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_DELETE)
    public Result<Void> deleteRegion(@PathVariable Long id) {
        geoAppService.deleteRegion(id);
        return Result.ok();
    }

    @PostMapping("/check-ip")
    @PreAuthorize(Permissions.COMPLIANCE_GEO_LIST)
    public Result<GeoCheckResult> checkIp(@Valid @RequestBody GeoCheckIpRequest request) {
        return Result.ok(geoAppService.checkIp(request.ip()));
    }
}
