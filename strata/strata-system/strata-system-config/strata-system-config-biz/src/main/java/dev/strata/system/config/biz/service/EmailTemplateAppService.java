// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.EmailTemplateDto;
import dev.strata.system.api.request.EmailTemplateCreateRequest;
import dev.strata.system.api.request.EmailTemplateUpdateRequest;
import dev.strata.system.config.api.entity.EmailTemplate;
import dev.strata.system.config.biz.mapper.EmailTemplateMapper;
import dev.strata.system.config.biz.repository.EmailTemplateRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailTemplateAppService {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateAppService.class);

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateMapper emailTemplateMapper;

    public EmailTemplateAppService(
            EmailTemplateRepository emailTemplateRepository, EmailTemplateMapper emailTemplateMapper) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailTemplateMapper = emailTemplateMapper;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateDto> list() {
        return emailTemplateRepository.findAllByOrderByIdAsc().stream()
                .map(emailTemplateMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<EmailTemplateDto> page(int page, int size) {
        return PageResult.from(emailTemplateRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(emailTemplateMapper::toDto));
    }

    @Transactional(readOnly = true)
    public EmailTemplateDto get(Long id) {
        return emailTemplateMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:emailtemplate", action = "create")
    public EmailTemplateDto create(EmailTemplateCreateRequest request) {
        if (emailTemplateRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Email template code already exists");
        }
        EmailTemplate template = emailTemplateMapper.toEntity(request);
        emailTemplateRepository.save(template);
        return emailTemplateMapper.toDto(template);
    }

    @Transactional
    @OperationLog(module = "system:emailtemplate", action = "update")
    public EmailTemplateDto update(Long id, EmailTemplateUpdateRequest request) {
        EmailTemplate template = require(id);
        emailTemplateMapper.update(request, template);
        emailTemplateRepository.save(template);
        return emailTemplateMapper.toDto(template);
    }

    @Transactional
    @OperationLog(module = "system:emailtemplate", action = "delete")
    public void delete(Long id) {
        emailTemplateRepository.delete(require(id));
    }

    private EmailTemplate require(Long id) {
        return emailTemplateRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Email template not found"));
    }
}
