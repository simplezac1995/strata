// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.AnnouncementDto;
import dev.strata.system.api.request.AnnouncementCreateRequest;
import dev.strata.system.api.request.AnnouncementUpdateRequest;
import dev.strata.system.communication.biz.service.AnnouncementAppService;
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
@RequestMapping("/api/v1/system/announcements")
public class AnnouncementController {

    private final AnnouncementAppService announcementAppService;

    public AnnouncementController(AnnouncementAppService announcementAppService) {
        this.announcementAppService = announcementAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_ANNOUNCEMENT_LIST)
    public Result<List<AnnouncementDto>> list() {
        return Result.ok(announcementAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_ANNOUNCEMENT_LIST)
    public Result<PageResult<AnnouncementDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(announcementAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_ANNOUNCEMENT_LIST)
    public Result<AnnouncementDto> get(@PathVariable Long id) {
        return Result.ok(announcementAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_ANNOUNCEMENT_CREATE)
    public Result<AnnouncementDto> create(@Valid @RequestBody AnnouncementCreateRequest request) {
        return Result.ok(announcementAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_ANNOUNCEMENT_UPDATE)
    public Result<AnnouncementDto> update(
            @PathVariable Long id, @Valid @RequestBody AnnouncementUpdateRequest request) {
        return Result.ok(announcementAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_ANNOUNCEMENT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        announcementAppService.delete(id);
        return Result.ok();
    }
}
