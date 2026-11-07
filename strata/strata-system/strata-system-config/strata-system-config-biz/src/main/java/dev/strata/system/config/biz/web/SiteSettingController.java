// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.config.biz.service.SiteSettingAppService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/site-settings")
public class SiteSettingController {

    private final SiteSettingAppService siteSettingAppService;

    public SiteSettingController(SiteSettingAppService siteSettingAppService) {
        this.siteSettingAppService = siteSettingAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_SITESETTING_LIST)
    public Result<Map<String, String>> list() {
        return Result.ok(siteSettingAppService.getAllAsMap());
    }

    @PutMapping
    @PreAuthorize(Permissions.SYSTEM_SITESETTING_UPDATE)
    public Result<Map<String, String>> putAll(@RequestBody Map<String, String> body) {
        return Result.ok(siteSettingAppService.putAll(body));
    }
}
