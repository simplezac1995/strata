// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import dev.strata.ai.api.request.AiKnowledgeCreateRequest;
import dev.strata.ai.api.request.AiKnowledgeUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiKnowledgeDto;
import dev.strata.ai.knowledge.api.entity.AiKnowledge;
import dev.strata.ai.knowledge.biz.mapper.AiKnowledgeMapper;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.AiKnowledgeRepository;
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
public class AiKnowledgeAppService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeAppService.class);

    private final AiKnowledgeRepository aiKnowledgeRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final AiKnowledgeMapper aiKnowledgeMapper;

    public AiKnowledgeAppService(
            AiKnowledgeRepository aiKnowledgeRepository,
            AiDocumentRepository aiDocumentRepository,
            AiKnowledgeMapper aiKnowledgeMapper) {
        this.aiKnowledgeRepository = aiKnowledgeRepository;
        this.aiDocumentRepository = aiDocumentRepository;
        this.aiKnowledgeMapper = aiKnowledgeMapper;
    }

    @Transactional(readOnly = true)
    public List<AiKnowledgeDto> list() {
        return aiKnowledgeRepository.findAllByOrderByIdAsc().stream()
                .map(aiKnowledgeMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiKnowledgeDto> page(int page, int size) {
        return PageResult.from(aiKnowledgeRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiKnowledgeMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiKnowledgeDto get(Long id) {
        return aiKnowledgeMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:knowledge", action = "create")
    public AiKnowledgeDto create(AiKnowledgeCreateRequest request) {
        AiKnowledge knowledge = aiKnowledgeMapper.toEntity(request);
        aiKnowledgeRepository.save(knowledge);
        return aiKnowledgeMapper.toDto(knowledge);
    }

    @Transactional
    @OperationLog(module = "ai:knowledge", action = "update")
    public AiKnowledgeDto update(Long id, AiKnowledgeUpdateRequest request) {
        AiKnowledge knowledge = require(id);
        aiKnowledgeMapper.update(request, knowledge);
        aiKnowledgeRepository.save(knowledge);
        return aiKnowledgeMapper.toDto(knowledge);
    }

    @Transactional
    @OperationLog(module = "ai:knowledge", action = "delete")
    public void delete(Long id) {
        AiKnowledge knowledge = require(id);
        aiDocumentRepository.deleteByKnowledgeId(id);
        aiKnowledgeRepository.delete(knowledge);
    }

    private AiKnowledge require(Long id) {
        return aiKnowledgeRepository.findById(id).orElseThrow(() -> new NotFoundException("Knowledge base not found"));
    }
}
