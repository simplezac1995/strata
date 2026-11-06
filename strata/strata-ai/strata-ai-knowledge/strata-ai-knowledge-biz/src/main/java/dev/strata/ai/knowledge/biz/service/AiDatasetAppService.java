// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import dev.strata.ai.api.request.AiDatasetCreateRequest;
import dev.strata.ai.api.request.AiDatasetUpdateRequest;
import dev.strata.ai.knowledge.api.dto.AiDatasetDto;
import dev.strata.ai.knowledge.api.entity.AiDataset;
import dev.strata.ai.knowledge.biz.mapper.AiDatasetMapper;
import dev.strata.ai.knowledge.biz.repository.AiDatasetRepository;
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
public class AiDatasetAppService {

    private static final Logger log = LoggerFactory.getLogger(AiDatasetAppService.class);

    private final AiDatasetRepository aiDatasetRepository;
    private final AiDatasetMapper aiDatasetMapper;

    public AiDatasetAppService(AiDatasetRepository aiDatasetRepository, AiDatasetMapper aiDatasetMapper) {
        this.aiDatasetRepository = aiDatasetRepository;
        this.aiDatasetMapper = aiDatasetMapper;
    }

    @Transactional(readOnly = true)
    public List<AiDatasetDto> list() {
        return aiDatasetRepository.findAllByOrderByIdAsc().stream()
                .map(aiDatasetMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiDatasetDto> page(int page, int size) {
        return PageResult.from(aiDatasetRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiDatasetMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiDatasetDto get(Long id) {
        return aiDatasetMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:dataset", action = "create")
    public AiDatasetDto create(AiDatasetCreateRequest request) {
        AiDataset dataset = aiDatasetMapper.toEntity(request);
        aiDatasetRepository.save(dataset);
        return aiDatasetMapper.toDto(dataset);
    }

    @Transactional
    @OperationLog(module = "ai:dataset", action = "update")
    public AiDatasetDto update(Long id, AiDatasetUpdateRequest request) {
        AiDataset dataset = require(id);
        aiDatasetMapper.update(request, dataset);
        aiDatasetRepository.save(dataset);
        return aiDatasetMapper.toDto(dataset);
    }

    @Transactional
    @OperationLog(module = "ai:dataset", action = "delete")
    public void delete(Long id) {
        aiDatasetRepository.delete(require(id));
    }

    private AiDataset require(Long id) {
        return aiDatasetRepository.findById(id).orElseThrow(() -> new NotFoundException("Dataset not found"));
    }
}
