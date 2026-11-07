// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.NotificationPrefDto;
import dev.strata.system.api.request.NotificationPrefUpdateRequest;
import dev.strata.system.communication.biz.service.NotificationPrefAppService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/notification-preferences")
public class NotificationPrefController {

    private final NotificationPrefAppService notificationPrefAppService;

    public NotificationPrefController(NotificationPrefAppService notificationPrefAppService) {
        this.notificationPrefAppService = notificationPrefAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<NotificationPrefDto> get() {
        return Result.ok(notificationPrefAppService.getCurrent());
    }

    @PutMapping
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<NotificationPrefDto> update(@Valid @RequestBody NotificationPrefUpdateRequest request) {
        return Result.ok(notificationPrefAppService.updateCurrent(request));
    }
}
