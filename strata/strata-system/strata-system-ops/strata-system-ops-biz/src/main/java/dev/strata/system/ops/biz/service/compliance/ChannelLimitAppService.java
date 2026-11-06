// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.compliance.ChannelLimitDto;
import dev.strata.system.api.request.compliance.ChannelLimitCreateRequest;
import dev.strata.system.api.request.compliance.ChannelLimitUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.ChannelLimit;
import dev.strata.system.ops.biz.mapper.compliance.ChannelLimitMapper;
import dev.strata.system.ops.biz.repository.compliance.ChannelLimitRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deposit/withdraw channel limits. */
@Service
public class ChannelLimitAppService {

    private static final Logger log = LoggerFactory.getLogger(ChannelLimitAppService.class);

    private final ChannelLimitRepository channelLimitRepository;
    private final ChannelLimitMapper channelLimitMapper;

    public ChannelLimitAppService(
            ChannelLimitRepository channelLimitRepository, ChannelLimitMapper channelLimitMapper) {
        this.channelLimitRepository = channelLimitRepository;
        this.channelLimitMapper = channelLimitMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<ChannelLimitDto> page(String channel, String direction, int page, int size) {
        var spec = Specs.of(ChannelLimit.class)
                .eq("channel", channel)
                .eq("direction", direction)
                .toSpecification();
        Page<ChannelLimit> result =
                channelLimitRepository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(channelLimitMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<ChannelLimitDto> myChannels() {
        return channelLimitRepository.findByEnabledTrue().stream()
                .map(channelLimitMapper::toDto)
                .toList();
    }

    @Transactional
    @OperationLog(module = "compliance:channel", action = "create")
    public ChannelLimitDto create(ChannelLimitCreateRequest request) {
        if (channelLimitRepository.existsByChannelAndDirection(request.channel(), request.direction())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Channel limit already exists for this direction");
        }
        ChannelLimit row = channelLimitMapper.toEntity(request);
        channelLimitRepository.save(row);
        return channelLimitMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:channel", action = "update")
    public ChannelLimitDto update(Long id, ChannelLimitUpdateRequest request) {
        ChannelLimit row = require(id);
        channelLimitMapper.update(request, row);
        channelLimitRepository.save(row);
        return channelLimitMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:channel", action = "delete")
    public void delete(Long id) {
        channelLimitRepository.delete(require(id));
    }

    private ChannelLimit require(Long id) {
        return channelLimitRepository.findById(id).orElseThrow(() -> new NotFoundException("Channel limit not found"));
    }
}
