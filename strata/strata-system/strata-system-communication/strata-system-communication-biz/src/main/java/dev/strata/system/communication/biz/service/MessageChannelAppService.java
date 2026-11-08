// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.MessageChannelDto;
import dev.strata.system.api.request.MessageChannelUpdateRequest;
import dev.strata.system.communication.api.entity.MessageChannel;
import dev.strata.system.communication.biz.mapper.MessageChannelMapper;
import dev.strata.system.communication.biz.repository.MessageChannelRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageChannelAppService {

    private static final Logger log = LoggerFactory.getLogger(MessageChannelAppService.class);

    private final MessageChannelRepository messageChannelRepository;
    private final MessageChannelMapper messageChannelMapper;

    public MessageChannelAppService(
            MessageChannelRepository messageChannelRepository, MessageChannelMapper messageChannelMapper) {
        this.messageChannelRepository = messageChannelRepository;
        this.messageChannelMapper = messageChannelMapper;
    }

    @Transactional(readOnly = true)
    public List<MessageChannelDto> list() {
        return messageChannelRepository.findAllByOrderByIdAsc().stream()
                .map(messageChannelMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<MessageChannelDto> page(int page, int size) {
        return PageResult.from(messageChannelRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(messageChannelMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "system:messagechannel", action = "update")
    public MessageChannelDto updateByType(String channelType, MessageChannelUpdateRequest request) {
        MessageChannel channel = messageChannelRepository
                .findByChannelType(channelType)
                .orElseThrow(() -> new NotFoundException("Message channel not found"));
        if (request.configJson() != null) {
            channel.setConfigJson(request.configJson());
        }
        if (request.enabled() != null) {
            channel.setEnabled(request.enabled());
        }
        messageChannelRepository.save(channel);
        return messageChannelMapper.toDto(channel);
    }
}
