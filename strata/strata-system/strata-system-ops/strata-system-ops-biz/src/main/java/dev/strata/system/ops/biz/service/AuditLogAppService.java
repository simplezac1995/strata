// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.AuditLogDto;
import dev.strata.system.api.event.AuditLogEvent;
import dev.strata.system.ops.api.entity.AuditLog;
import dev.strata.system.ops.biz.mapper.AuditLogMapper;
import dev.strata.system.ops.biz.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogAppService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAppService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogAppService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AuditLogDto> page(int page, int size) {
        Page<AuditLog> result =
                auditLogRepository.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(auditLogMapper::toDto));
    }

    /** Persist audit events independently so failed business transactions remain auditable. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(AuditLogEvent event) {
        AuditLog log = new AuditLog();
        log.setEventType(event.eventType());
        log.setModuleName(event.module());
        log.setActionName(event.action());
        log.setResourceType(event.resourceType());
        log.setResourceId(event.resourceId());
        log.setOperatorId(event.operatorId());
        log.setOperatorName(event.operatorName());
        log.setIpAddress(event.ipAddress());
        log.setUserAgent(event.userAgent());
        log.setSuccess(event.success());
        log.setDetail(event.detail());
        auditLogRepository.save(log);
    }
}
