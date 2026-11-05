// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.TenantDto;
import dev.strata.system.api.request.TenantCreateRequest;
import dev.strata.system.api.request.TenantUpdateRequest;
import dev.strata.system.rbac.api.entity.Tenant;
import dev.strata.system.rbac.biz.mapper.TenantMapper;
import dev.strata.system.rbac.biz.repository.TenantRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAppService {

    private static final Logger log = LoggerFactory.getLogger(TenantAppService.class);

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    public TenantAppService(TenantRepository tenantRepository, TenantMapper tenantMapper) {
        this.tenantRepository = tenantRepository;
        this.tenantMapper = tenantMapper;
    }

    @Transactional(readOnly = true)
    public List<TenantDto> list() {
        return tenantRepository.findAllByOrderByIdAsc().stream()
                .map(tenantMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<TenantDto> page(int page, int size) {
        return PageResult.from(tenantRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(tenantMapper::toDto));
    }

    @Transactional(readOnly = true)
    public TenantDto get(Long id) {
        return tenantMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:tenant", action = "create")
    public TenantDto create(TenantCreateRequest request) {
        if (tenantRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Tenant code already exists");
        }
        Tenant tenant = tenantMapper.toEntity(request);
        tenantRepository.save(tenant);
        return tenantMapper.toDto(tenant);
    }

    @Transactional
    @OperationLog(module = "system:tenant", action = "update")
    public TenantDto update(Long id, TenantUpdateRequest request) {
        Tenant tenant = require(id);
        tenantMapper.update(request, tenant);
        tenantRepository.save(tenant);
        return tenantMapper.toDto(tenant);
    }

    @Transactional
    @OperationLog(module = "system:tenant", action = "delete")
    public void delete(Long id) {
        tenantRepository.delete(require(id));
    }

    private Tenant require(Long id) {
        return tenantRepository.findById(id).orElseThrow(() -> new NotFoundException("Tenant not found"));
    }
}
