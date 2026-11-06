// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiProviderCreateRequest;
import dev.strata.ai.api.request.AiProviderUpdateRequest;
import dev.strata.ai.config.api.dto.AiProviderDto;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.mapper.AiProviderMapper;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
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
public class AiProviderAppService {

    private static final Logger log = LoggerFactory.getLogger(AiProviderAppService.class);

    private final AiProviderRepository aiProviderRepository;
    private final AiProviderMapper aiProviderMapper;

    public AiProviderAppService(AiProviderRepository aiProviderRepository, AiProviderMapper aiProviderMapper) {
        this.aiProviderRepository = aiProviderRepository;
        this.aiProviderMapper = aiProviderMapper;
    }

    @Transactional(readOnly = true)
    public List<AiProviderDto> list() {
        return aiProviderRepository.findAll().stream()
                .map(aiProviderMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiProviderDto> page(int page, int size) {
        return PageResult.from(aiProviderRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiProviderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiProviderDto get(Long id) {
        return aiProviderMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:provider", action = "create")
    @AuditLog(
            action = "AI_PROVIDER_CREATE",
            description = "Create AI provider",
            sensitive = true,
            category = "AI_CONFIG")
    public AiProviderDto create(AiProviderCreateRequest request) {
        requireCodeFree(request.code());
        AiProvider provider = aiProviderMapper.toEntity(request);
        aiProviderRepository.save(provider);
        return aiProviderMapper.toDto(provider);
    }

    @Transactional
    @OperationLog(module = "ai:provider", action = "update")
    @AuditLog(
            action = "AI_PROVIDER_UPDATE",
            description = "Update AI provider",
            sensitive = true,
            category = "AI_CONFIG")
    public AiProviderDto update(Long id, AiProviderUpdateRequest request) {
        AiProvider provider = require(id);
        aiProviderMapper.update(request, provider);
        aiProviderRepository.save(provider);
        return aiProviderMapper.toDto(provider);
    }

    @Transactional
    @OperationLog(module = "ai:provider", action = "delete")
    @AuditLog(
            action = "AI_PROVIDER_DELETE",
            description = "Delete AI provider",
            sensitive = true,
            category = "AI_CONFIG")
    public void delete(Long id) {
        aiProviderRepository.delete(require(id));
    }

    private void requireCodeFree(String code) {
        if (aiProviderRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Ai provider code already exists");
        }
    }

    private AiProvider require(Long id) {
        return aiProviderRepository.findById(id).orElseThrow(() -> new NotFoundException("Ai provider not found"));
    }
}
