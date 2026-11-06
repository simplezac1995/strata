// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiPromptCreateRequest;
import dev.strata.ai.api.request.AiPromptUpdateRequest;
import dev.strata.ai.config.api.dto.AiPromptDto;
import dev.strata.ai.config.api.entity.AiPrompt;
import dev.strata.ai.config.biz.mapper.AiPromptMapper;
import dev.strata.ai.config.biz.repository.AiPromptRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
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
public class AiPromptAppService {

    private static final Logger log = LoggerFactory.getLogger(AiPromptAppService.class);

    private final AiPromptRepository aiPromptRepository;
    private final AiPromptMapper aiPromptMapper;

    public AiPromptAppService(AiPromptRepository aiPromptRepository, AiPromptMapper aiPromptMapper) {
        this.aiPromptRepository = aiPromptRepository;
        this.aiPromptMapper = aiPromptMapper;
    }

    @Transactional(readOnly = true)
    public List<AiPromptDto> list() {
        return aiPromptRepository.findAllByOrderByIdAsc().stream()
                .map(aiPromptMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiPromptDto> page(int page, int size) {
        return PageResult.from(aiPromptRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiPromptMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiPromptDto get(Long id) {
        return aiPromptMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:prompt", action = "create")
    public AiPromptDto create(AiPromptCreateRequest request) {
        requireCodeFree(request.code());
        AiPrompt prompt = aiPromptMapper.toEntity(request);
        aiPromptRepository.save(prompt);
        return aiPromptMapper.toDto(prompt);
    }

    @Transactional
    @OperationLog(module = "ai:prompt", action = "update")
    public AiPromptDto update(Long id, AiPromptUpdateRequest request) {
        AiPrompt prompt = require(id);
        aiPromptMapper.update(request, prompt);
        aiPromptRepository.save(prompt);
        return aiPromptMapper.toDto(prompt);
    }

    @Transactional
    @OperationLog(module = "ai:prompt", action = "delete")
    public void delete(Long id) {
        aiPromptRepository.delete(require(id));
    }

    private void requireCodeFree(String code) {
        if (aiPromptRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Ai prompt code already exists");
        }
    }

    private AiPrompt require(Long id) {
        return aiPromptRepository.findById(id).orElseThrow(() -> new NotFoundException("Ai prompt not found"));
    }
}
