// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.WebhookDto;
import dev.strata.system.api.request.WebhookCreateRequest;
import dev.strata.system.api.request.WebhookUpdateRequest;
import dev.strata.system.communication.api.entity.Webhook;
import dev.strata.system.communication.biz.mapper.WebhookMapper;
import dev.strata.system.communication.biz.repository.WebhookRepository;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookAppService {

    private static final Logger log = LoggerFactory.getLogger(WebhookAppService.class);

    private final WebhookRepository webhookRepository;
    private final WebhookMapper webhookMapper;

    public WebhookAppService(WebhookRepository webhookRepository, WebhookMapper webhookMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookMapper = webhookMapper;
    }

    @Transactional(readOnly = true)
    public List<WebhookDto> list() {
        return webhookRepository.findAllByOrderByIdAsc().stream()
                .map(webhookMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<WebhookDto> page(int page, int size) {
        return PageResult.from(webhookRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(webhookMapper::toDto));
    }

    @Transactional(readOnly = true)
    public WebhookDto get(Long id) {
        return webhookMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:webhook", action = "create")
    public WebhookDto create(WebhookCreateRequest request) {
        Webhook webhook = webhookMapper.toEntity(request);
        /* Only the hash is stored — the plaintext secret is the caller's to keep. */
        if (request.secret() != null && !request.secret().isBlank()) {
            webhook.setSecret(DigestUtils.sha256Hex(request.secret()));
        }
        webhookRepository.save(webhook);
        return webhookMapper.toDto(webhook);
    }

    @Transactional
    @OperationLog(module = "system:webhook", action = "update")
    public WebhookDto update(Long id, WebhookUpdateRequest request) {
        Webhook webhook = require(id);
        webhookMapper.update(request, webhook);
        /* A new secret rotates the stored hash; null/blank leaves the current one unchanged. */
        if (request.secret() != null && !request.secret().isBlank()) {
            webhook.setSecret(DigestUtils.sha256Hex(request.secret()));
        }
        webhookRepository.save(webhook);
        return webhookMapper.toDto(webhook);
    }

    @Transactional
    @OperationLog(module = "system:webhook", action = "delete")
    public void delete(Long id) {
        webhookRepository.delete(require(id));
    }

    private Webhook require(Long id) {
        return webhookRepository.findById(id).orElseThrow(() -> new NotFoundException("Webhook not found"));
    }
}
