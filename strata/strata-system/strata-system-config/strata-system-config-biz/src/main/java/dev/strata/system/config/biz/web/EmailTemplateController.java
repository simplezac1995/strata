// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.EmailTemplateDto;
import dev.strata.system.api.request.EmailTemplateCreateRequest;
import dev.strata.system.api.request.EmailTemplateUpdateRequest;
import dev.strata.system.config.biz.service.EmailTemplateAppService;
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
@RequestMapping("/api/v1/system/email-templates")
public class EmailTemplateController {

    private final EmailTemplateAppService emailTemplateAppService;

    public EmailTemplateController(EmailTemplateAppService emailTemplateAppService) {
        this.emailTemplateAppService = emailTemplateAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_EMAILTEMPLATE_LIST)
    public Result<List<EmailTemplateDto>> list() {
        return Result.ok(emailTemplateAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_EMAILTEMPLATE_LIST)
    public Result<PageResult<EmailTemplateDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(emailTemplateAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_EMAILTEMPLATE_LIST)
    public Result<EmailTemplateDto> get(@PathVariable Long id) {
        return Result.ok(emailTemplateAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_EMAILTEMPLATE_CREATE)
    public Result<EmailTemplateDto> create(@Valid @RequestBody EmailTemplateCreateRequest request) {
        return Result.ok(emailTemplateAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_EMAILTEMPLATE_UPDATE)
    public Result<EmailTemplateDto> update(
            @PathVariable Long id, @Valid @RequestBody EmailTemplateUpdateRequest request) {
        return Result.ok(emailTemplateAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_EMAILTEMPLATE_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        emailTemplateAppService.delete(id);
        return Result.ok();
    }
}
