// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.RiskAlertDto;
import dev.strata.system.ops.api.entity.RiskAlert;
import dev.strata.system.ops.biz.mapper.RiskAlertMapper;
import dev.strata.system.ops.biz.repository.RiskAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskAlertAppService {

    private static final Logger log = LoggerFactory.getLogger(RiskAlertAppService.class);

    private final RiskAlertRepository riskAlertRepository;
    private final RiskAlertMapper riskAlertMapper;

    public RiskAlertAppService(RiskAlertRepository riskAlertRepository, RiskAlertMapper riskAlertMapper) {
        this.riskAlertRepository = riskAlertRepository;
        this.riskAlertMapper = riskAlertMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<RiskAlertDto> page(String status, int page, int size) {
        var spec = Specs.of(RiskAlert.class).eq("status", status).toSpecification();
        Page<RiskAlert> result =
                riskAlertRepository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(riskAlertMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "system:risk", action = "update")
    public RiskAlertDto resolve(Long id) {
        RiskAlert alert = require(id);
        if (!"open".equals(alert.getStatus()) && !"ignored".equals(alert.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "Alert is already " + alert.getStatus());
        }
        alert.setStatus("resolved");
        riskAlertRepository.save(alert);
        return riskAlertMapper.toDto(alert);
    }

    private RiskAlert require(Long id) {
        return riskAlertRepository.findById(id).orElseThrow(() -> new NotFoundException("Risk alert not found"));
    }
}
