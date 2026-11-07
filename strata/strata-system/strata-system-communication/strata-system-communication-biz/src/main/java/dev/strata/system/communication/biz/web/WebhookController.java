// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.WebhookDto;
import dev.strata.system.api.request.WebhookCreateRequest;
import dev.strata.system.api.request.WebhookUpdateRequest;
import dev.strata.system.communication.biz.service.WebhookAppService;
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
@RequestMapping("/api/v1/system/webhooks")
public class WebhookController {

    private final WebhookAppService webhookAppService;

    public WebhookController(WebhookAppService webhookAppService) {
        this.webhookAppService = webhookAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_WEBHOOK_LIST)
    public Result<List<WebhookDto>> list() {
        return Result.ok(webhookAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_WEBHOOK_LIST)
    public Result<PageResult<WebhookDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(webhookAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_WEBHOOK_LIST)
    public Result<WebhookDto> get(@PathVariable Long id) {
        return Result.ok(webhookAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_WEBHOOK_CREATE)
    public Result<WebhookDto> create(@Valid @RequestBody WebhookCreateRequest request) {
        return Result.ok(webhookAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_WEBHOOK_UPDATE)
    public Result<WebhookDto> update(@PathVariable Long id, @Valid @RequestBody WebhookUpdateRequest request) {
        return Result.ok(webhookAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_WEBHOOK_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        webhookAppService.delete(id);
        return Result.ok();
    }
}
