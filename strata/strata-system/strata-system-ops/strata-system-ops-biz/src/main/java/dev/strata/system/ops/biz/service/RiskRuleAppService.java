// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.RiskRuleDto;
import dev.strata.system.api.request.RiskRuleCreateRequest;
import dev.strata.system.api.request.RiskRuleUpdateRequest;
import dev.strata.system.ops.api.entity.RiskRule;
import dev.strata.system.ops.biz.mapper.RiskRuleMapper;
import dev.strata.system.ops.biz.repository.RiskRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskRuleAppService {

    private static final Logger log = LoggerFactory.getLogger(RiskRuleAppService.class);

    private final RiskRuleRepository riskRuleRepository;
    private final RiskRuleMapper riskRuleMapper;

    public RiskRuleAppService(RiskRuleRepository riskRuleRepository, RiskRuleMapper riskRuleMapper) {
        this.riskRuleRepository = riskRuleRepository;
        this.riskRuleMapper = riskRuleMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<RiskRuleDto> page(String type, Boolean enabled, int page, int size) {
        var spec =
                Specs.of(RiskRule.class).eq("type", type).eq("enabled", enabled).toSpecification();
        Page<RiskRule> result =
                riskRuleRepository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(riskRuleMapper::toDto));
    }

    @Transactional(readOnly = true)
    public RiskRuleDto get(Long id) {
        return riskRuleMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:risk", action = "create")
    public RiskRuleDto create(RiskRuleCreateRequest request) {
        RiskRule rule = riskRuleMapper.toEntity(request);
        riskRuleRepository.save(rule);
        return riskRuleMapper.toDto(rule);
    }

    @Transactional
    @OperationLog(module = "system:risk", action = "update")
    public RiskRuleDto update(Long id, RiskRuleUpdateRequest request) {
        RiskRule rule = require(id);
        riskRuleMapper.update(request, rule);
        riskRuleRepository.save(rule);
        return riskRuleMapper.toDto(rule);
    }

    @Transactional
    @OperationLog(module = "system:risk", action = "delete")
    public void delete(Long id) {
        riskRuleRepository.delete(require(id));
    }

    private RiskRule require(Long id) {
        return riskRuleRepository.findById(id).orElseThrow(() -> new NotFoundException("Risk rule not found"));
    }
}
