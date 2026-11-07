// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.ConversationDto;
import dev.strata.system.api.dto.CsMessageDto;
import dev.strata.system.api.request.CsReplyRequest;
import dev.strata.system.communication.biz.service.CsConversationAppService;
import jakarta.validation.Valid;
import java.util.List;
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

/**
 * Staff customer-service workbench. Player-side endpoints (the H5 chain) are out of scope this batch; only
 * the employee list / thread / reply surface is wired here.
 */
@Validated
@RestController
@RequestMapping("/api/v1/system/cs/conversations")
public class CsConversationController {

    private final CsConversationAppService csConversationAppService;

    public CsConversationController(CsConversationAppService csConversationAppService) {
        this.csConversationAppService = csConversationAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_CS_LIST)
    public Result<PageResult<ConversationDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(csConversationAppService.page(status, page, size));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize(Permissions.SYSTEM_CS_LIST)
    public Result<List<CsMessageDto>> messages(@PathVariable Long id) {
        return Result.ok(csConversationAppService.messages(id));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize(Permissions.SYSTEM_CS_CREATE)
    public Result<CsMessageDto> reply(
            @PathVariable Long id, @Valid @RequestBody CsReplyRequest request, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(csConversationAppService.reply(id, principal.userId(), request));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_CS_CREATE)
    public Result<ConversationDto> open(@RequestParam Long playerId) {
        return Result.ok(csConversationAppService.open(playerId));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize(Permissions.SYSTEM_CS_CREATE)
    public Result<ConversationDto> close(@PathVariable Long id) {
        return Result.ok(csConversationAppService.close(id));
    }
}
