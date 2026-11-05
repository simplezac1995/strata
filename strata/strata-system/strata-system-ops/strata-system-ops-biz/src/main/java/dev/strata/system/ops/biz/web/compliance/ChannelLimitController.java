// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.compliance.ChannelLimitDto;
import dev.strata.system.api.request.compliance.ChannelLimitCreateRequest;
import dev.strata.system.api.request.compliance.ChannelLimitUpdateRequest;
import dev.strata.system.ops.biz.service.compliance.ChannelLimitAppService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin channel-limit management; player read of the admin-configured deposit/withdraw limits.
 *
 * <p>Deliberately NOT mounted at {@code /api/v1/player/wallet/channels}: that path is already owned by the
 * casino {@code PlayerWalletApiController}, which projects usable channels from the asset-network registry.
 * This endpoint exposes the separate {@code channel_limit} table (min/max/daily/fee), so it lives one path over.
 */
@Validated
@RestController
public class ChannelLimitController {

    private final ChannelLimitAppService channelLimitAppService;

    public ChannelLimitController(ChannelLimitAppService channelLimitAppService) {
        this.channelLimitAppService = channelLimitAppService;
    }

    @GetMapping("/api/v1/admin/channel-limits")
    @PreAuthorize(Permissions.COMPLIANCE_CHANNEL_LIST)
    public Result<PageResult<ChannelLimitDto>> page(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(channelLimitAppService.page(channel, direction, p, s));
    }

    @PostMapping("/api/v1/admin/channel-limits")
    @PreAuthorize(Permissions.COMPLIANCE_CHANNEL_CREATE)
    public Result<ChannelLimitDto> create(@Valid @RequestBody ChannelLimitCreateRequest request) {
        return Result.ok(channelLimitAppService.create(request));
    }

    @PutMapping("/api/v1/admin/channel-limits/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_CHANNEL_UPDATE)
    public Result<ChannelLimitDto> update(
            @PathVariable Long id, @Valid @RequestBody ChannelLimitUpdateRequest request) {
        return Result.ok(channelLimitAppService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/channel-limits/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_CHANNEL_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        channelLimitAppService.delete(id);
        return Result.ok();
    }

    @GetMapping("/api/v1/player/wallet/channel-limits")
    public Result<List<ChannelLimitDto>> myChannels() {
        return Result.ok(channelLimitAppService.myChannels());
    }
}
