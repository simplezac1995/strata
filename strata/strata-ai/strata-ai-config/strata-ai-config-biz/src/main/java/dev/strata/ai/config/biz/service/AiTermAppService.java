// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiTermCreateRequest;
import dev.strata.ai.api.request.AiTermUpdateRequest;
import dev.strata.ai.config.api.dto.AiTermDto;
import dev.strata.ai.config.api.entity.AiTerm;
import dev.strata.ai.config.biz.mapper.AiTermMapper;
import dev.strata.ai.config.biz.repository.AiTermRepository;
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
public class AiTermAppService {

    private static final Logger log = LoggerFactory.getLogger(AiTermAppService.class);

    private final AiTermRepository aiTermRepository;
    private final AiTermMapper aiTermMapper;

    public AiTermAppService(AiTermRepository aiTermRepository, AiTermMapper aiTermMapper) {
        this.aiTermRepository = aiTermRepository;
        this.aiTermMapper = aiTermMapper;
    }

    @Transactional(readOnly = true)
    public List<AiTermDto> list() {
        return aiTermRepository.findAllByOrderByIdAsc().stream()
                .map(aiTermMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiTermDto> page(int page, int size) {
        return PageResult.from(aiTermRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiTermMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiTermDto get(Long id) {
        return aiTermMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:term", action = "create")
    public AiTermDto create(AiTermCreateRequest request) {
        requireTermFree(request.term());
        AiTerm term = aiTermMapper.toEntity(request);
        aiTermRepository.save(term);
        return aiTermMapper.toDto(term);
    }

    @Transactional
    @OperationLog(module = "ai:term", action = "update")
    public AiTermDto update(Long id, AiTermUpdateRequest request) {
        AiTerm term = require(id);
        aiTermMapper.update(request, term);
        aiTermRepository.save(term);
        return aiTermMapper.toDto(term);
    }

    @Transactional
    @OperationLog(module = "ai:term", action = "delete")
    public void delete(Long id) {
        aiTermRepository.delete(require(id));
    }

    private void requireTermFree(String term) {
        if (aiTermRepository.existsByTerm(term)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Ai term already exists");
        }
    }

    private AiTerm require(Long id) {
        return aiTermRepository.findById(id).orElseThrow(() -> new NotFoundException("Ai term not found"));
    }
}
