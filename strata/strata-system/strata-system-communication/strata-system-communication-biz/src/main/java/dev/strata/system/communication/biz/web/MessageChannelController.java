// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.MessageChannelDto;
import dev.strata.system.api.request.MessageChannelUpdateRequest;
import dev.strata.system.communication.biz.service.MessageChannelAppService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/message-channels")
public class MessageChannelController {

    private final MessageChannelAppService messageChannelAppService;

    public MessageChannelController(MessageChannelAppService messageChannelAppService) {
        this.messageChannelAppService = messageChannelAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_MESSAGECHANNEL_LIST)
    public Result<List<MessageChannelDto>> list() {
        return Result.ok(messageChannelAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_MESSAGECHANNEL_LIST)
    public Result<PageResult<MessageChannelDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(messageChannelAppService.page(page, size));
    }

    @PutMapping("/{channelType}")
    @PreAuthorize(Permissions.SYSTEM_MESSAGECHANNEL_UPDATE)
    public Result<MessageChannelDto> update(
            @PathVariable String channelType, @Valid @RequestBody MessageChannelUpdateRequest request) {
        return Result.ok(messageChannelAppService.updateByType(channelType, request));
    }
}
