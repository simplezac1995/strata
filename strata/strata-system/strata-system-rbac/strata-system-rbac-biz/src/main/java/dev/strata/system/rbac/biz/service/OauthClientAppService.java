// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.OauthClientDto;
import dev.strata.system.api.request.OauthClientCreateRequest;
import dev.strata.system.api.request.OauthClientUpdateRequest;
import dev.strata.system.rbac.api.entity.OauthClient;
import dev.strata.system.rbac.biz.mapper.OauthClientMapper;
import dev.strata.system.rbac.biz.repository.OauthClientRepository;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OauthClientAppService {

    private static final Logger log = LoggerFactory.getLogger(OauthClientAppService.class);

    private final OauthClientRepository oauthClientRepository;
    private final OauthClientMapper oauthClientMapper;

    public OauthClientAppService(OauthClientRepository oauthClientRepository, OauthClientMapper oauthClientMapper) {
        this.oauthClientRepository = oauthClientRepository;
        this.oauthClientMapper = oauthClientMapper;
    }

    @Transactional(readOnly = true)
    public List<OauthClientDto> list() {
        return oauthClientRepository.findAllByOrderByIdAsc().stream()
                .map(oauthClientMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<OauthClientDto> page(int page, int size) {
        return PageResult.from(oauthClientRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(oauthClientMapper::toDto));
    }

    @Transactional(readOnly = true)
    public OauthClientDto get(Long id) {
        return oauthClientMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:client", action = "create")
    public OauthClientDto create(OauthClientCreateRequest request) {
        if (oauthClientRepository.existsByClientId(request.clientId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Client id already exists");
        }
        OauthClient client = oauthClientMapper.toEntity(request);
        /* Only the hash is stored 鈥?the plaintext secret is the caller's to keep. */
        client.setClientSecret(DigestUtils.sha256Hex(request.clientSecret()));
        oauthClientRepository.save(client);
        return oauthClientMapper.toDto(client);
    }

    @Transactional
    @OperationLog(module = "system:client", action = "update")
    public OauthClientDto update(Long id, OauthClientUpdateRequest request) {
        OauthClient client = require(id);
        oauthClientMapper.update(request, client);
        /* A new secret rotates the stored hash; null/blank leaves the current one unchanged. */
        if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
            client.setClientSecret(DigestUtils.sha256Hex(request.clientSecret()));
        }
        oauthClientRepository.save(client);
        return oauthClientMapper.toDto(client);
    }

    @Transactional
    @OperationLog(module = "system:client", action = "delete")
    public void delete(Long id) {
        oauthClientRepository.delete(require(id));
    }

    private OauthClient require(Long id) {
        return oauthClientRepository.findById(id).orElseThrow(() -> new NotFoundException("Client not found"));
    }
}
