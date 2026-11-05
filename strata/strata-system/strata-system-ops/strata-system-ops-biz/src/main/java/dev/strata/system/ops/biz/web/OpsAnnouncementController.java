// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.OpsAnnouncementDto;
import dev.strata.system.api.request.OpsAnnouncementUpsertRequest;
import dev.strata.system.ops.biz.service.OpsAnnouncementAppService;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ops/announcements")
public class OpsAnnouncementController {

    private final OpsAnnouncementAppService announcementAppService;

    public OpsAnnouncementController(OpsAnnouncementAppService announcementAppService) {
        this.announcementAppService = announcementAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.OPS_ANNOUNCEMENT_LIST)
    public Result<List<OpsAnnouncementDto>> list() {
        return Result.ok(announcementAppService.list());
    }

    @GetMapping("/active")
    @PreAuthorize(Permissions.OPS_ANNOUNCEMENT_LIST)
    public Result<List<OpsAnnouncementDto>> active() {
        return Result.ok(announcementAppService.active());
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.OPS_ANNOUNCEMENT_LIST)
    public Result<OpsAnnouncementDto> get(@PathVariable Long id) {
        return Result.ok(announcementAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.OPS_ANNOUNCEMENT_CREATE)
    public Result<OpsAnnouncementDto> create(@Valid @RequestBody OpsAnnouncementUpsertRequest request) {
        return Result.ok(announcementAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.OPS_ANNOUNCEMENT_UPDATE)
    public Result<OpsAnnouncementDto> update(
            @PathVariable Long id, @Valid @RequestBody OpsAnnouncementUpsertRequest request) {
        return Result.ok(announcementAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.OPS_ANNOUNCEMENT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        announcementAppService.delete(id);
        return Result.ok();
    }
}
