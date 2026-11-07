// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.PopupConfigDto;
import dev.strata.system.api.request.PopupUpsertRequest;
import dev.strata.system.ops.biz.service.PopupConfigAppService;
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
@RequestMapping("/api/v1/ops/popups")
public class PopupConfigController {

    private final PopupConfigAppService popupConfigAppService;

    public PopupConfigController(PopupConfigAppService popupConfigAppService) {
        this.popupConfigAppService = popupConfigAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.OPS_POPUP_LIST)
    public Result<List<PopupConfigDto>> list() {
        return Result.ok(popupConfigAppService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.OPS_POPUP_LIST)
    public Result<PopupConfigDto> get(@PathVariable Long id) {
        return Result.ok(popupConfigAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.OPS_POPUP_CREATE)
    public Result<PopupConfigDto> create(@Valid @RequestBody PopupUpsertRequest request) {
        return Result.ok(popupConfigAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.OPS_POPUP_UPDATE)
    public Result<PopupConfigDto> update(@PathVariable Long id, @Valid @RequestBody PopupUpsertRequest request) {
        return Result.ok(popupConfigAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.OPS_POPUP_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        popupConfigAppService.delete(id);
        return Result.ok();
    }
}
