// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.compliance.AmlReportDto;
import dev.strata.system.api.dto.compliance.AmlRuleDto;
import dev.strata.system.api.request.compliance.AmlReportReviewRequest;
import dev.strata.system.api.request.compliance.AmlRuleCreateRequest;
import dev.strata.system.api.request.compliance.AmlRuleUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.AmlReport;
import dev.strata.system.ops.api.entity.compliance.AmlRule;
import dev.strata.system.ops.biz.mapper.compliance.AmlReportMapper;
import dev.strata.system.ops.biz.mapper.compliance.AmlRuleMapper;
import dev.strata.system.ops.biz.repository.compliance.AmlReportRepository;
import dev.strata.system.ops.biz.repository.compliance.AmlRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AML rules and report review. Real transaction detection is TODO (CRUD this iteration only). */
@Service
public class AmlAppService {

    private static final Logger log = LoggerFactory.getLogger(AmlAppService.class);

    private final AmlRuleRepository amlRuleRepository;
    private final AmlRuleMapper amlRuleMapper;
    private final AmlReportRepository amlReportRepository;
    private final AmlReportMapper amlReportMapper;

    public AmlAppService(
            AmlRuleRepository amlRuleRepository,
            AmlRuleMapper amlRuleMapper,
            AmlReportRepository amlReportRepository,
            AmlReportMapper amlReportMapper) {
        this.amlRuleRepository = amlRuleRepository;
        this.amlRuleMapper = amlRuleMapper;
        this.amlReportRepository = amlReportRepository;
        this.amlReportMapper = amlReportMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AmlRuleDto> pageRules(String ruleType, Boolean enabled, int page, int size) {
        var spec = Specs.of(AmlRule.class)
                .eq("ruleType", ruleType)
                .eq("enabled", enabled)
                .toSpecification();
        Page<AmlRule> result =
                amlRuleRepository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(amlRuleMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "compliance:aml", action = "create")
    public AmlRuleDto createRule(AmlRuleCreateRequest request) {
        AmlRule row = amlRuleMapper.toEntity(request);
        amlRuleRepository.save(row);
        return amlRuleMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:aml", action = "update")
    public AmlRuleDto updateRule(Long id, AmlRuleUpdateRequest request) {
        AmlRule row = requireRule(id);
        amlRuleMapper.update(request, row);
        amlRuleRepository.save(row);
        return amlRuleMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:aml", action = "delete")
    public void deleteRule(Long id) {
        amlRuleRepository.delete(requireRule(id));
    }

    @Transactional(readOnly = true)
    public PageResult<AmlReportDto> pageReports(String status, int page, int size) {
        var spec = Specs.of(AmlReport.class).eq("status", status).toSpecification();
        Page<AmlReport> result = amlReportRepository.findAll(
                spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.from(result.map(amlReportMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "compliance:aml", action = "update")
    public AmlReportDto reviewReport(Long id, AmlReportReviewRequest request) {
        AmlReport row =
                amlReportRepository.findById(id).orElseThrow(() -> new NotFoundException("AML report not found"));
        row.setStatus(request.status());
        amlReportRepository.save(row);
        return amlReportMapper.toDto(row);
    }

    private AmlRule requireRule(Long id) {
        return amlRuleRepository.findById(id).orElseThrow(() -> new NotFoundException("AML rule not found"));
    }
}
