// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.RiskIpBlacklistDto;
import dev.strata.system.api.request.RiskIpBlacklistCreateRequest;
import dev.strata.system.ops.api.entity.RiskIpBlacklist;
import dev.strata.system.ops.biz.mapper.RiskIpBlacklistMapper;
import dev.strata.system.ops.biz.repository.RiskIpBlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskIpBlacklistAppService {

    private static final Logger log = LoggerFactory.getLogger(RiskIpBlacklistAppService.class);

    private final RiskIpBlacklistRepository riskIpBlacklistRepository;
    private final RiskIpBlacklistMapper riskIpBlacklistMapper;

    public RiskIpBlacklistAppService(
            RiskIpBlacklistRepository riskIpBlacklistRepository, RiskIpBlacklistMapper riskIpBlacklistMapper) {
        this.riskIpBlacklistRepository = riskIpBlacklistRepository;
        this.riskIpBlacklistMapper = riskIpBlacklistMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<RiskIpBlacklistDto> page(int page, int size) {
        var result = riskIpBlacklistRepository.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(riskIpBlacklistMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "system:risk", action = "create")
    public RiskIpBlacklistDto create(RiskIpBlacklistCreateRequest request) {
        String ip = request.ipAddress().trim();
        if (riskIpBlacklistRepository.existsByIpAddress(ip)) {
            throw new BusinessException(ErrorCode.CONFLICT, "IP already blacklisted");
        }
        RiskIpBlacklist row = riskIpBlacklistMapper.toEntity(request);
        row.setIpAddress(ip);
        riskIpBlacklistRepository.save(row);
        return riskIpBlacklistMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "system:risk", action = "delete")
    public void delete(Long id) {
        riskIpBlacklistRepository.delete(riskIpBlacklistRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Blacklisted IP not found")));
    }
}
