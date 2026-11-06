// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiAgentCreateRequest;
import dev.strata.ai.api.request.AiAgentUpdateRequest;
import dev.strata.ai.config.api.dto.AiAgentDto;
import dev.strata.ai.config.api.entity.AiAgent;
import dev.strata.ai.config.biz.mapper.AiAgentMapper;
import dev.strata.ai.config.biz.repository.AiAgentRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAgentAppService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentAppService.class);

    private final AiAgentRepository aiAgentRepository;
    private final AiAgentMapper aiAgentMapper;

    public AiAgentAppService(AiAgentRepository aiAgentRepository, AiAgentMapper aiAgentMapper) {
        this.aiAgentRepository = aiAgentRepository;
        this.aiAgentMapper = aiAgentMapper;
    }

    @Transactional(readOnly = true)
    public List<AiAgentDto> list() {
        return aiAgentRepository.findAllByOrderByIdAsc().stream()
                .map(aiAgentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiAgentDto> page(int page, int size) {
        return PageResult.from(aiAgentRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiAgentMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiAgentDto get(Long id) {
        return aiAgentMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:agent", action = "create")
    public AiAgentDto create(AiAgentCreateRequest request) {
        AiAgent agent = aiAgentMapper.toEntity(request);
        aiAgentRepository.save(agent);
        return aiAgentMapper.toDto(agent);
    }

    @Transactional
    @OperationLog(module = "ai:agent", action = "update")
    public AiAgentDto update(Long id, AiAgentUpdateRequest request) {
        AiAgent agent = require(id);
        aiAgentMapper.update(request, agent);
        aiAgentRepository.save(agent);
        return aiAgentMapper.toDto(agent);
    }

    @Transactional
    @OperationLog(module = "ai:agent", action = "delete")
    public void delete(Long id) {
        aiAgentRepository.delete(require(id));
    }

    private AiAgent require(Long id) {
        return aiAgentRepository.findById(id).orElseThrow(() -> new NotFoundException("Agent not found"));
    }
}
