// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.DictTypeDto;
import dev.strata.system.api.request.DictTypeCreateRequest;
import dev.strata.system.api.request.DictTypeUpdateRequest;
import dev.strata.system.config.biz.service.DictTypeAppService;
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
@RequestMapping("/api/v1/system/dict-types")
public class DictTypeController {

    private final DictTypeAppService dictTypeAppService;

    public DictTypeController(DictTypeAppService dictTypeAppService) {
        this.dictTypeAppService = dictTypeAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_DICT_LIST)
    public Result<List<DictTypeDto>> list() {
        return Result.ok(dictTypeAppService.list());
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_DICT_CREATE)
    public Result<DictTypeDto> create(@Valid @RequestBody DictTypeCreateRequest request) {
        return Result.ok(dictTypeAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_DICT_UPDATE)
    public Result<DictTypeDto> update(@PathVariable Long id, @Valid @RequestBody DictTypeUpdateRequest request) {
        return Result.ok(dictTypeAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_DICT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        dictTypeAppService.delete(id);
        return Result.ok();
    }
}
