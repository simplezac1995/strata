// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.CacheKeyDto;
import dev.strata.system.api.dto.CacheStatusDto;
import dev.strata.system.ops.biz.service.CacheMonitorAppService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/cache")
public class CacheMonitorController {

    private final CacheMonitorAppService cacheMonitorAppService;

    public CacheMonitorController(CacheMonitorAppService cacheMonitorAppService) {
        this.cacheMonitorAppService = cacheMonitorAppService;
    }

    @GetMapping("/status")
    @PreAuthorize(Permissions.SYSTEM_CACHE_LIST)
    public Result<CacheStatusDto> status() {
        return Result.ok(cacheMonitorAppService.status());
    }

    @GetMapping("/keys")
    @PreAuthorize(Permissions.SYSTEM_CACHE_LIST)
    public Result<List<CacheKeyDto>> keys(@RequestParam(required = false) String pattern) {
        return Result.ok(cacheMonitorAppService.listKeys(pattern));
    }

    @DeleteMapping("/keys")
    @PreAuthorize(Permissions.SYSTEM_CACHE_DELETE)
    public Result<Void> deleteKey(@RequestParam String key) {
        cacheMonitorAppService.deleteKey(key);
        return Result.ok();
    }

    @DeleteMapping("/flush")
    @PreAuthorize(Permissions.SYSTEM_CACHE_DELETE)
    public Result<Void> flush() {
        cacheMonitorAppService.flush();
        return Result.ok();
    }
}
