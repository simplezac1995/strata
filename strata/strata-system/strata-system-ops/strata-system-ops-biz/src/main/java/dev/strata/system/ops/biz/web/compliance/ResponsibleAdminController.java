// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.compliance.SelfExclusionDto;
import dev.strata.system.ops.biz.service.compliance.ResponsibleGamblingAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin oversight of responsible-gambling records. */
@Validated
@RestController
@RequestMapping("/api/v1/admin/responsible")
public class ResponsibleAdminController {

    private final ResponsibleGamblingAppService responsibleGamblingAppService;

    public ResponsibleAdminController(ResponsibleGamblingAppService responsibleGamblingAppService) {
        this.responsibleGamblingAppService = responsibleGamblingAppService;
    }

    @GetMapping("/users")
    @PreAuthorize(Permissions.COMPLIANCE_RESPONSIBLE_LIST)
    public Result<PageResult<SelfExclusionDto>> pageUsers(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(responsibleGamblingAppService.pageUsers(p, s));
    }
}
