// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.communication.biz.player.PlayerCsAppService;
import dev.strata.system.communication.biz.player.PlayerCsAppService.TicketMessageView;
import dev.strata.system.communication.biz.player.PlayerCsAppService.TicketView;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Player-facing customer-service tickets on the JWT player REST surface. */
@Validated
@RestController
@RequestMapping("/api/v1/player/cs/tickets")
public class PlayerCsController {

    private final PlayerCsAppService tickets;

    public PlayerCsController(PlayerCsAppService tickets) {
        this.tickets = tickets;
    }

    /** Opens a new ticket. */
    @PostMapping
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<TicketView> open(@RequestBody OpenTicketRequest request, Authentication authentication) {
        return Result.ok(
                tickets.open(playerId(authentication), request.category(), request.title(), request.content()));
    }

    /** My tickets. */
    @GetMapping
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<PageResult<TicketView>> myTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return Result.ok(tickets.myTickets(playerId(authentication), page, size));
    }

    /** One ticket's messages. */
    @GetMapping("/{id}/messages")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<List<TicketMessageView>> messages(@PathVariable Long id, Authentication authentication) {
        return Result.ok(tickets.messages(playerId(authentication), id));
    }

    /** Appends a message to one of my tickets. */
    @PostMapping("/{id}/messages")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public Result<TicketMessageView> reply(
            @PathVariable Long id, @RequestBody ReplyRequest request, Authentication authentication) {
        return Result.ok(tickets.reply(playerId(authentication), id, request.content()));
    }

    private static Long playerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Not authenticated");
        }
        return principal.userId();
    }

    /** {@code POST /tickets} body. */
    public record OpenTicketRequest(String category, String title, String content) {}

    /** {@code POST /{id}/messages} body. */
    public record ReplyRequest(String content) {}
}
