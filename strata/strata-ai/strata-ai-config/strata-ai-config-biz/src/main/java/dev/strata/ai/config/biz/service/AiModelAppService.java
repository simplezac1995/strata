// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiModelCreateRequest;
import dev.strata.ai.api.request.AiModelUpdateRequest;
import dev.strata.ai.config.api.dto.AiModelDto;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.biz.mapper.AiModelMapper;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.security.audit.AuditLog;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiModelAppService {

    private static final Logger log = LoggerFactory.getLogger(AiModelAppService.class);

    private final AiModelRepository aiModelRepository;
    private final AiModelMapper aiModelMapper;

    public AiModelAppService(AiModelRepository aiModelRepository, AiModelMapper aiModelMapper) {
        this.aiModelRepository = aiModelRepository;
        this.aiModelMapper = aiModelMapper;
    }

    @Transactional(readOnly = true)
    public List<AiModelDto> list() {
        return aiModelRepository.findAllByOrderByIdAsc().stream()
                .map(aiModelMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiModelDto> page(int page, int size) {
        return PageResult.from(aiModelRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiModelMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiModelDto get(Long id) {
        return aiModelMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:model", action = "create")
    @AuditLog(action = "AI_MODEL_CREATE", description = "Create AI model", category = "AI_CONFIG")
    public AiModelDto create(AiModelCreateRequest request) {
        requireCodeFree(request.providerId(), request.code());
        AiModel model = aiModelMapper.toEntity(request);
        aiModelRepository.save(model);
        return aiModelMapper.toDto(model);
    }

    @Transactional
    @OperationLog(module = "ai:model", action = "update")
    @AuditLog(action = "AI_MODEL_UPDATE", description = "Update AI model", category = "AI_CONFIG")
    public AiModelDto update(Long id, AiModelUpdateRequest request) {
        AiModel model = require(id);
        aiModelMapper.update(request, model);
        aiModelRepository.save(model);
        return aiModelMapper.toDto(model);
    }

    @Transactional
    @OperationLog(module = "ai:model", action = "delete")
    @AuditLog(action = "AI_MODEL_DELETE", description = "Delete AI model", category = "AI_CONFIG")
    public void delete(Long id) {
        aiModelRepository.delete(require(id));
    }

    private void requireCodeFree(Long providerId, String code) {
        if (aiModelRepository.existsByProviderIdAndCode(providerId, code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Ai model already exists");
        }
    }

    private AiModel require(Long id) {
        return aiModelRepository.findById(id).orElseThrow(() -> new NotFoundException("Ai model not found"));
    }
}
