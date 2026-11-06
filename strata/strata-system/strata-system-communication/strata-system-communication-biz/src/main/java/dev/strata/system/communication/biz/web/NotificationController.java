// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.NotificationDto;
import dev.strata.system.api.request.NotificationBroadcastRequest;
import dev.strata.system.communication.biz.service.NotificationAppService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in staff member's own notification inbox, plus the admin-only broadcast. */
@Validated
@RestController
@RequestMapping("/api/v1/system/notifications")
public class NotificationController {

    private final NotificationAppService notificationAppService;

    public NotificationController(NotificationAppService notificationAppService) {
        this.notificationAppService = notificationAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_NOTIFICATION_LIST)
    public Result<PageResult<NotificationDto>> list(
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(notificationAppService.listForUser(principal.userId(), onlyUnread, page, size));
    }

    @GetMapping("/unread-count")
    @PreAuthorize(Permissions.SYSTEM_NOTIFICATION_LIST)
    public Result<Map<String, Long>> unreadCount(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(Map.of("count", notificationAppService.unreadCount(principal.userId())));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize(Permissions.SYSTEM_NOTIFICATION_LIST)
    public Result<Void> markRead(@PathVariable Long id, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        notificationAppService.markRead(principal.userId(), id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    @PreAuthorize(Permissions.SYSTEM_NOTIFICATION_LIST)
    public Result<Void> markAllRead(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        notificationAppService.markAllRead(principal.userId());
        return Result.ok();
    }

    @PostMapping("/broadcast")
    @PreAuthorize(Permissions.SYSTEM_NOTIFICATION_CREATE)
    public Result<Map<String, Integer>> broadcast(@Valid @RequestBody NotificationBroadcastRequest request) {
        int sent = notificationAppService.broadcast(request);
        return Result.ok(Map.of("sent", sent));
    }
}
