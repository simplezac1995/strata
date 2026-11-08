// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import dev.strata.ai.api.request.AiDocumentCreateRequest;
import dev.strata.ai.api.request.AiDocumentUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDocumentDto;
import dev.strata.ai.knowledge.api.entity.AiDocument;
import dev.strata.ai.knowledge.biz.mapper.AiDocumentMapper;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.ai.knowledge.biz.repository.AiKnowledgeRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiDocumentAppService {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentAppService.class);

    private final AiDocumentRepository aiDocumentRepository;
    private final AiKnowledgeRepository aiKnowledgeRepository;
    private final AiDocumentMapper aiDocumentMapper;

    public AiDocumentAppService(
            AiDocumentRepository aiDocumentRepository,
            AiKnowledgeRepository aiKnowledgeRepository,
            AiDocumentMapper aiDocumentMapper) {
        this.aiDocumentRepository = aiDocumentRepository;
        this.aiKnowledgeRepository = aiKnowledgeRepository;
        this.aiDocumentMapper = aiDocumentMapper;
    }

    @Transactional(readOnly = true)
    public List<AiDocumentDto> listByKnowledge(Long knowledgeId) {
        requireKnowledge(knowledgeId);
        return aiDocumentRepository.findAllByKnowledgeIdOrderByIdAsc(knowledgeId).stream()
                .map(aiDocumentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AiDocumentDto> listAll() {
        // P2-22: un-paged endpoint is hard-capped at 50 rows.
        return aiDocumentRepository.findTop50ByOrderByIdAsc().stream()
                .map(aiDocumentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiDocumentDto get(Long id) {
        return aiDocumentMapper.toDto(require(id));
    }

    @Transactional(readOnly = true)
    public PageResult<AiDocumentDto> page(Long knowledgeId, Integer page, Integer size) {
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<AiDocument> result = knowledgeId == null
                ? aiDocumentRepository.findAll(pageable)
                : aiDocumentRepository.findByKnowledgeId(knowledgeId, pageable);
        return PageResult.from(result.map(aiDocumentMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "ai:document", action = "create")
    public AiDocumentDto create(AiDocumentCreateRequest request) {
        requireKnowledge(request.knowledgeId());
        AiDocument document = aiDocumentMapper.toEntity(request);
        aiDocumentRepository.save(document);
        return aiDocumentMapper.toDto(document);
    }

    @Transactional
    @OperationLog(module = "ai:document", action = "update")
    public AiDocumentDto update(Long id, AiDocumentUpdateRequest request) {
        AiDocument document = require(id);
        aiDocumentMapper.update(request, document);
        aiDocumentRepository.save(document);
        return aiDocumentMapper.toDto(document);
    }

    @Transactional
    @OperationLog(module = "ai:document", action = "delete")
    public void delete(Long id) {
        aiDocumentRepository.delete(require(id));
    }

    private void requireKnowledge(Long knowledgeId) {
        if (!aiKnowledgeRepository.existsById(knowledgeId)) {
            throw new NotFoundException("Knowledge base not found");
        }
    }

    private AiDocument require(Long id) {
        return aiDocumentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
    }
}
