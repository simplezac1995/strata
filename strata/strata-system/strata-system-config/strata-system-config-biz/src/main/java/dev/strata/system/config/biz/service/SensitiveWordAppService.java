// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.SensitiveWordDto;
import dev.strata.system.api.request.SensitiveWordCreateRequest;
import dev.strata.system.api.request.SensitiveWordUpdateRequest;
import dev.strata.system.config.api.entity.SensitiveWord;
import dev.strata.system.config.biz.mapper.SensitiveWordMapper;
import dev.strata.system.config.biz.repository.SensitiveWordRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveWordAppService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordAppService.class);

    private static final String WHITELIST_PREFIX = "allow:";

    private final SensitiveWordRepository sensitiveWordRepository;
    private final SensitiveWordMapper sensitiveWordMapper;

    public SensitiveWordAppService(
            SensitiveWordRepository sensitiveWordRepository, SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordRepository = sensitiveWordRepository;
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @Transactional(readOnly = true)
    public List<SensitiveWordDto> list() {
        return sensitiveWordRepository.findAllByOrderByIdAsc().stream()
                .map(sensitiveWordMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<SensitiveWordDto> page(String category, int page, int size) {
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<SensitiveWord> result =
                switch (category == null ? "all" : category) {
                    case "all" -> sensitiveWordRepository.findAll(pageable);
                    case "sensitive" -> sensitiveWordRepository.findByWordNotStartingWith(WHITELIST_PREFIX, pageable);
                    case "whitelist" -> sensitiveWordRepository.findByWordStartingWith(WHITELIST_PREFIX, pageable);
                    default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid sensitive-word category");
                };
        return PageResult.from(result.map(sensitiveWordMapper::toDto));
    }

    @Transactional(readOnly = true)
    public SensitiveWordDto get(Long id) {
        return sensitiveWordMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:sensitiveword", action = "create")
    public SensitiveWordDto create(SensitiveWordCreateRequest request) {
        if (sensitiveWordRepository.existsByWord(request.word())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Sensitive word already exists");
        }
        SensitiveWord word = sensitiveWordMapper.toEntity(request);
        sensitiveWordRepository.save(word);
        return sensitiveWordMapper.toDto(word);
    }

    @Transactional
    @OperationLog(module = "system:sensitiveword", action = "update")
    public SensitiveWordDto update(Long id, SensitiveWordUpdateRequest request) {
        SensitiveWord word = require(id);
        sensitiveWordMapper.update(request, word);
        sensitiveWordRepository.save(word);
        return sensitiveWordMapper.toDto(word);
    }

    @Transactional
    @OperationLog(module = "system:sensitiveword", action = "delete")
    public void delete(Long id) {
        sensitiveWordRepository.delete(require(id));
    }

    private SensitiveWord require(Long id) {
        return sensitiveWordRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Sensitive word not found"));
    }
}
