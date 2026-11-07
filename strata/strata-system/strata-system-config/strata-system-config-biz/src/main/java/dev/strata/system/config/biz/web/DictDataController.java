// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.DictDataDto;
import dev.strata.system.api.request.DictDataCreateRequest;
import dev.strata.system.api.request.DictDataUpdateRequest;
import dev.strata.system.config.biz.service.DictDataAppService;
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
@RequestMapping("/api/v1/system/dict-data")
public class DictDataController {

    private final DictDataAppService dictDataAppService;

    public DictDataController(DictDataAppService dictDataAppService) {
        this.dictDataAppService = dictDataAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_DICT_LIST)
    public Result<List<DictDataDto>> listByType(@RequestParam Long typeId) {
        return Result.ok(dictDataAppService.listByType(typeId));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_DICT_CREATE)
    public Result<DictDataDto> create(@Valid @RequestBody DictDataCreateRequest request) {
        return Result.ok(dictDataAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_DICT_UPDATE)
    public Result<DictDataDto> update(@PathVariable Long id, @Valid @RequestBody DictDataUpdateRequest request) {
        return Result.ok(dictDataAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_DICT_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        dictDataAppService.delete(id);
        return Result.ok();
    }
}
