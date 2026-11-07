// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
import dev.strata.system.api.dto.AuditLogChangeDto;
import dev.strata.system.ops.api.entity.AuditLogChange;
import dev.strata.system.ops.biz.mapper.AuditLogChangeMapper;
import dev.strata.system.ops.biz.repository.AuditLogChangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogChangeAppService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogChangeAppService.class);

    private final AuditLogChangeRepository auditLogChangeRepository;
    private final AuditLogChangeMapper auditLogChangeMapper;

    public AuditLogChangeAppService(
            AuditLogChangeRepository auditLogChangeRepository, AuditLogChangeMapper auditLogChangeMapper) {
        this.auditLogChangeRepository = auditLogChangeRepository;
        this.auditLogChangeMapper = auditLogChangeMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AuditLogChangeDto> page(String entityName, String entityId, int page, int size) {
        var spec = Specs.of(AuditLogChange.class)
                .eq("entityName", entityName)
                .eq("entityId", entityId)
                .toSpecification();
        Page<AuditLogChange> result =
                auditLogChangeRepository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(auditLogChangeMapper::toDto));
    }

    /**
     * Roll back a single field change by writing {@code oldValue} back to the target entity.
     *
     * <p>TODO: perform the actual write-back (resolve the entity by {@code entityName}/{@code entityId}, apply
     * {@code oldValue} to {@code fieldName}, re-audit). This batch only ships the endpoint shell per scope; it
     * resolves and returns the change row without mutating the target entity.
     */
    @Transactional
    @OperationLog(module = "system:audit", action = "update")
    public AuditLogChangeDto rollback(Long id) {
        AuditLogChange change = auditLogChangeRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Audit change not found"));
        log.info(
                "Audit change rollback requested (shell): changeId={} entity={}#{} field={}",
                change.getId(),
                change.getEntityName(),
                change.getEntityId(),
                change.getFieldName());
        return auditLogChangeMapper.toDto(change);
    }
}
