// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.compliance.CoolOffDto;
import dev.strata.system.api.dto.compliance.ResponsibleGamblingLimitDto;
import dev.strata.system.api.dto.compliance.ResponsibleLimitsView;
import dev.strata.system.api.dto.compliance.SelfExclusionDto;
import dev.strata.system.api.request.compliance.CoolOffCreateRequest;
import dev.strata.system.api.request.compliance.ResponsibleLimitCreateRequest;
import dev.strata.system.api.request.compliance.SelfExclusionCreateRequest;
import dev.strata.system.ops.api.entity.compliance.CoolOff;
import dev.strata.system.ops.api.entity.compliance.ResponsibleGamblingLimit;
import dev.strata.system.ops.api.entity.compliance.SelfExclusion;
import dev.strata.system.ops.biz.mapper.compliance.CoolOffMapper;
import dev.strata.system.ops.biz.mapper.compliance.ResponsibleGamblingLimitMapper;
import dev.strata.system.ops.biz.mapper.compliance.SelfExclusionMapper;
import dev.strata.system.ops.biz.repository.compliance.CoolOffRepository;
import dev.strata.system.ops.biz.repository.compliance.ResponsibleGamblingLimitRepository;
import dev.strata.system.ops.biz.repository.compliance.SelfExclusionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsible gambling tools: limits, self-exclusion and cool-off. */
@Service
public class ResponsibleGamblingAppService {

    private static final Logger log = LoggerFactory.getLogger(ResponsibleGamblingAppService.class);

    private final ResponsibleGamblingLimitRepository limitRepository;
    private final ResponsibleGamblingLimitMapper limitMapper;
    private final SelfExclusionRepository selfExclusionRepository;
    private final SelfExclusionMapper selfExclusionMapper;
    private final CoolOffRepository coolOffRepository;
    private final CoolOffMapper coolOffMapper;

    public ResponsibleGamblingAppService(
            ResponsibleGamblingLimitRepository limitRepository,
            ResponsibleGamblingLimitMapper limitMapper,
            SelfExclusionRepository selfExclusionRepository,
            SelfExclusionMapper selfExclusionMapper,
            CoolOffRepository coolOffRepository,
            CoolOffMapper coolOffMapper) {
        this.limitRepository = limitRepository;
        this.limitMapper = limitMapper;
        this.selfExclusionRepository = selfExclusionRepository;
        this.selfExclusionMapper = selfExclusionMapper;
        this.coolOffRepository = coolOffRepository;
        this.coolOffMapper = coolOffMapper;
    }

    @Transactional(readOnly = true)
    public ResponsibleLimitsView myLimits(Long userId) {
        var limits = limitRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(limitMapper::toDto)
                .toList();
        var exclusions = selfExclusionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(selfExclusionMapper::toDto)
                .toList();
        var coolOffs = coolOffRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(coolOffMapper::toDto)
                .toList();
        return new ResponsibleLimitsView(limits, exclusions, coolOffs);
    }

    @Transactional
    @OperationLog(module = "compliance:responsible", action = "create")
    public ResponsibleGamblingLimitDto createLimit(Long userId, ResponsibleLimitCreateRequest request) {
        ResponsibleGamblingLimit row = limitMapper.toEntity(request);
        row.setUserId(userId);
        row.setStartDate(Instant.now());
        limitRepository.save(row);
        return limitMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:responsible", action = "create")
    public SelfExclusionDto selfExclude(Long userId, SelfExclusionCreateRequest request) {
        Instant now = Instant.now();
        SelfExclusion row = new SelfExclusion();
        row.setUserId(userId);
        row.setStartDate(now);
        row.setReason(request.reason());
        // null days => permanent (no end date), the safe default per ADR-0033.
        if (request.days() != null) {
            row.setEndDate(now.plus(request.days(), ChronoUnit.DAYS));
        }
        selfExclusionRepository.save(row);
        return selfExclusionMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:responsible", action = "create")
    public CoolOffDto coolOff(Long userId, CoolOffCreateRequest request) {
        CoolOff row = new CoolOff();
        row.setUserId(userId);
        row.setEndDate(Instant.now().plus(request.hours(), ChronoUnit.HOURS));
        coolOffRepository.save(row);
        return coolOffMapper.toDto(row);
    }

    /** Admin oversight: paged view of self-exclusions across users. */
    @Transactional(readOnly = true)
    public PageResult<SelfExclusionDto> pageUsers(int page, int size) {
        var result =
                selfExclusionRepository.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.from(result.map(selfExclusionMapper::toDto));
    }
}
