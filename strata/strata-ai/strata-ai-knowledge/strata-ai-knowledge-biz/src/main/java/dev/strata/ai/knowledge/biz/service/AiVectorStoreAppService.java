// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import dev.strata.ai.api.request.AiVectorStoreCreateRequest;
import dev.strata.ai.api.request.AiVectorStoreUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiVectorStoreDto;
import dev.strata.ai.knowledge.api.entity.AiVectorStore;
import dev.strata.ai.knowledge.biz.mapper.AiVectorStoreMapper;
import dev.strata.ai.knowledge.biz.repository.AiVectorStoreRepository;
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
public class AiVectorStoreAppService {

    private static final Logger log = LoggerFactory.getLogger(AiVectorStoreAppService.class);

    private final AiVectorStoreRepository aiVectorStoreRepository;
    private final AiVectorStoreMapper aiVectorStoreMapper;

    public AiVectorStoreAppService(
            AiVectorStoreRepository aiVectorStoreRepository, AiVectorStoreMapper aiVectorStoreMapper) {
        this.aiVectorStoreRepository = aiVectorStoreRepository;
        this.aiVectorStoreMapper = aiVectorStoreMapper;
    }

    @Transactional(readOnly = true)
    public List<AiVectorStoreDto> list() {
        return aiVectorStoreRepository.findAllByOrderByIdAsc().stream()
                .map(aiVectorStoreMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiVectorStoreDto> page(int page, int size) {
        return PageResult.from(aiVectorStoreRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiVectorStoreMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiVectorStoreDto get(Long id) {
        return aiVectorStoreMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:vectorstore", action = "create")
    public AiVectorStoreDto create(AiVectorStoreCreateRequest request) {
        AiVectorStore vectorStore = aiVectorStoreMapper.toEntity(request);
        aiVectorStoreRepository.save(vectorStore);
        return aiVectorStoreMapper.toDto(vectorStore);
    }

    @Transactional
    @OperationLog(module = "ai:vectorstore", action = "update")
    public AiVectorStoreDto update(Long id, AiVectorStoreUpdateRequest request) {
        AiVectorStore vectorStore = require(id);
        aiVectorStoreMapper.update(request, vectorStore);
        aiVectorStoreRepository.save(vectorStore);
        return aiVectorStoreMapper.toDto(vectorStore);
    }

    @Transactional
    @OperationLog(module = "ai:vectorstore", action = "delete")
    public void delete(Long id) {
        aiVectorStoreRepository.delete(require(id));
    }

    private AiVectorStore require(Long id) {
        return aiVectorStoreRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Ai vector store not found"));
    }
}
