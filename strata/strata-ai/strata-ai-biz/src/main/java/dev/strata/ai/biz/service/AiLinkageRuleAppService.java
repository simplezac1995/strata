// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import dev.strata.ai.api.dto.AiLinkageRuleDto;
import dev.strata.ai.api.request.AiLinkageRuleUpsertRequest;
import dev.strata.ai.biz.entity.AiLinkageRule;
import dev.strata.ai.biz.event.LinkageTestEvent;
import dev.strata.ai.biz.mapper.AiLinkageRuleMapper;
import dev.strata.ai.biz.repository.AiLinkageRuleRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD for AI linkage rules ({@code ai:linkage:*}). */
@Service
public class AiLinkageRuleAppService {

    private final AiLinkageRuleRepository repository;
    private final AiLinkageRuleMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public AiLinkageRuleAppService(
            AiLinkageRuleRepository repository, AiLinkageRuleMapper mapper, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResult<AiLinkageRuleDto> page(int page, int size) {
        Page<AiLinkageRule> result =
                repository.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(mapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<AiLinkageRuleDto> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    @OperationLog(module = "ai:linkage", action = "create")
    public AiLinkageRuleDto create(AiLinkageRuleUpsertRequest request) {
        AiLinkageRule rule = mapper.toEntity(request);
        repository.save(rule);
        return mapper.toDto(rule);
    }

    @Transactional
    @OperationLog(module = "ai:linkage", action = "update")
    public AiLinkageRuleDto update(Long id, AiLinkageRuleUpsertRequest request) {
        AiLinkageRule rule = require(id);
        mapper.update(request, rule);
        repository.save(rule);
        return mapper.toDto(rule);
    }

    @Transactional
    @OperationLog(module = "ai:linkage", action = "setEnabled")
    public void setEnabled(Long id, boolean enabled) {
        AiLinkageRule rule = require(id);
        rule.setEnabled(enabled);
        repository.save(rule);
    }

    @Transactional
    @OperationLog(module = "ai:linkage", action = "delete")
    public void delete(Long id) {
        repository.delete(require(id));
    }

    /** Fire a test event for the given rule so admins can verify notify/tag/email wiring. */
    @Transactional
    @OperationLog(module = "ai:linkage", action = "test")
    public void testRule(Long id) {
        AiLinkageRule rule = require(id);
        eventPublisher.publishEvent(new LinkageTestEvent(
                rule.getObject(), rule.getEvent(), "TEST: rule [" + rule.getName() + "] fired from admin console"));
    }

    public record LinkageTestEvent(String object, String event, String detail) {}

    private AiLinkageRule require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("AI linkage rule not found"));
    }
}
