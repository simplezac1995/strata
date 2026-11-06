// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.NotificationDto;
import dev.strata.system.communication.biz.service.NotificationAppService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Player notification inbox on the JWT player REST surface. Thin self-scoped wrapper over the existing
 * {@link NotificationAppService}; the staff inbox on {@code /api/v1/system/notifications} is untouched.
 */
@Validated
@RestController
@RequestMapping("/api/v1/player/notifications")
public class PlayerNotificationController {

    private final NotificationAppService notifications;

    public PlayerNotificationController(NotificationAppService notifications) {
        this.notifications = notifications;
    }

    /** My notifications, optionally only unread. */
    @GetMapping
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<PageResult<NotificationDto>> list(
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return Result.ok(notifications.listForUser(playerId(authentication), onlyUnread, page, size));
    }

    /** How many unread notifications I have. */
    @GetMapping("/unread-count")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<Map<String, Long>> unreadCount(Authentication authentication) {
        return Result.ok(Map.of("count", notifications.unreadCount(playerId(authentication))));
    }

    /** Marks one notification as read. */
    @PutMapping("/{id}/read")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<Void> markRead(@PathVariable Long id, Authentication authentication) {
        notifications.markRead(playerId(authentication), id);
        return Result.ok();
    }

    /** Marks all my notifications as read. */
    @PutMapping("/read-all")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<Void> markAllRead(Authentication authentication) {
        notifications.markAllRead(playerId(authentication));
        return Result.ok();
    }

    private static Long playerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Not authenticated");
        }
        return principal.userId();
    }
}
