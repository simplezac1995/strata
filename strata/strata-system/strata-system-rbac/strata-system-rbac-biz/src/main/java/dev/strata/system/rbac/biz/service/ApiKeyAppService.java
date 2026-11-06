// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.ApiKeyCreatedDto;
import dev.strata.system.api.dto.ApiKeyDto;
import dev.strata.system.api.dto.ApiKeySummaryDto;
import dev.strata.system.api.request.ApiKeyCreateRequest;
import dev.strata.system.api.request.ApiKeyUpdateRequest;
import dev.strata.system.rbac.api.entity.ApiKey;
import dev.strata.system.rbac.biz.mapper.ApiKeyMapper;
import dev.strata.system.rbac.biz.repository.ApiKeyRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyAppService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAppService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyAppService(ApiKeyRepository apiKeyRepository, ApiKeyMapper apiKeyMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyMapper = apiKeyMapper;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> list() {
        return apiKeyRepository.findByDeletedFalseOrderByIdAsc().stream()
                .map(apiKeyMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<ApiKeyDto> page(int page, int size) {
        return PageResult.from(apiKeyRepository
                .findByDeletedFalse(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(apiKeyMapper::toDto));
    }

    @Transactional(readOnly = true)
    public ApiKeySummaryDto summary() {
        return new ApiKeySummaryDto(
                apiKeyRepository.countByDeletedFalse(), apiKeyRepository.countByDeletedFalseAndEnabledTrue());
    }

    @Transactional(readOnly = true)
    public ApiKeyDto get(Long id) {
        return apiKeyMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:apikey", action = "create")
    public ApiKeyCreatedDto create(ApiKeyCreateRequest request) {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String secret = HexFormat.of().formatHex(bytes);
        String plaintext = "sk_" + secret;
        String prefix = plaintext.substring(0, Math.min(10, plaintext.length()));

        ApiKey key = new ApiKey();
        key.setName(request.name());
        key.setKeyPrefix(prefix);
        key.setKeyHash(DigestUtils.sha256Hex(plaintext));
        key.setScopes(request.scopes());
        key.setEnabled(request.enabled() == null || request.enabled());
        apiKeyRepository.save(key);

        return new ApiKeyCreatedDto(
                String.valueOf(key.getId()),
                key.getName(),
                key.getKeyPrefix(),
                plaintext,
                key.getScopes(),
                key.isEnabled(),
                key.getCreatedAt());
    }

    @Transactional
    @OperationLog(module = "system:apikey", action = "update")
    public ApiKeyDto update(Long id, ApiKeyUpdateRequest request) {
        ApiKey key = require(id);
        if (request.name() != null && !request.name().isBlank()) {
            key.setName(request.name());
        }
        if (request.scopes() != null) {
            key.setScopes(request.scopes());
        }
        if (request.enabled() != null) {
            key.setEnabled(request.enabled());
        }
        apiKeyRepository.save(key);
        return apiKeyMapper.toDto(key);
    }

    @Transactional
    @OperationLog(module = "system:apikey", action = "delete")
    public void delete(Long id) {
        ApiKey key = require(id);
        key.setDeleted(true);
        apiKeyRepository.save(key);
    }

    private ApiKey require(Long id) {
        return apiKeyRepository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("API key not found"));
    }
}
