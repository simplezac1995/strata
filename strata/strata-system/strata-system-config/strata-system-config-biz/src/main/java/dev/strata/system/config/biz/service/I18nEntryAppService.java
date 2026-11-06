// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.I18nEntryDto;
import dev.strata.system.api.request.I18nEntryCreateRequest;
import dev.strata.system.api.request.I18nEntryUpdateRequest;
import dev.strata.system.config.api.entity.I18nEntry;
import dev.strata.system.config.biz.mapper.I18nEntryMapper;
import dev.strata.system.config.biz.repository.I18nEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class I18nEntryAppService {

    private static final Logger log = LoggerFactory.getLogger(I18nEntryAppService.class);

    private final I18nEntryRepository i18nEntryRepository;
    private final I18nEntryMapper i18nEntryMapper;

    public I18nEntryAppService(I18nEntryRepository i18nEntryRepository, I18nEntryMapper i18nEntryMapper) {
        this.i18nEntryRepository = i18nEntryRepository;
        this.i18nEntryMapper = i18nEntryMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<I18nEntryDto> page(String keyword, Integer page, Integer size) {
        Specification<I18nEntry> base =
                Specs.of(I18nEntry.class).eq("deleted", false).toSpecification();
        if (keyword != null && !keyword.isBlank()) {
            Specification<I18nEntry> filter = Specs.of(I18nEntry.class)
                    .like("messageKey", keyword)
                    .toSpecification()
                    .or(Specs.of(I18nEntry.class).like("zh", keyword).toSpecification())
                    .or(Specs.of(I18nEntry.class).like("en", keyword).toSpecification());
            base = base.and(filter);
        }
        Page<I18nEntry> result =
                i18nEntryRepository.findAll(base, PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return PageResult.from(result.map(i18nEntryMapper::toDto));
    }

    @Transactional(readOnly = true)
    public I18nEntryDto get(Long id) {
        return i18nEntryMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:i18n", action = "create")
    public I18nEntryDto create(I18nEntryCreateRequest request) {
        if (i18nEntryRepository.existsByMessageKeyAndDeletedFalse(request.messageKey())) {
            throw new BusinessException(ErrorCode.CONFLICT, "I18n message key already exists");
        }
        I18nEntry entry = i18nEntryMapper.toEntity(request);
        i18nEntryRepository.save(entry);
        return i18nEntryMapper.toDto(entry);
    }

    @Transactional
    @OperationLog(module = "system:i18n", action = "update")
    public I18nEntryDto update(Long id, I18nEntryUpdateRequest request) {
        I18nEntry entry = require(id);
        i18nEntryMapper.update(request, entry);
        i18nEntryRepository.save(entry);
        return i18nEntryMapper.toDto(entry);
    }

    @Transactional
    @OperationLog(module = "system:i18n", action = "delete")
    public void delete(Long id) {
        I18nEntry entry = require(id);
        entry.setDeleted(true);
        i18nEntryRepository.save(entry);
    }

    private I18nEntry require(Long id) {
        return i18nEntryRepository
                .findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new NotFoundException("I18n entry not found"));
    }
}
