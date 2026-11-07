// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.OperationLogDto;
import dev.strata.system.ops.api.entity.OperationLog;
import dev.strata.system.ops.biz.mapper.OperationLogMapper;
import dev.strata.system.ops.biz.repository.OperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationLogAppService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAppService.class);

    private final OperationLogRepository operationLogRepository;
    private final OperationLogMapper operationLogMapper;

    public OperationLogAppService(
            OperationLogRepository operationLogRepository, OperationLogMapper operationLogMapper) {
        this.operationLogRepository = operationLogRepository;
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<OperationLogDto> page(int page, int size) {
        Page<OperationLog> result =
                operationLogRepository.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(operationLogMapper::toDto));
    }

    /** The current user's own operation log (personal center), filtered by {@code operatorId}. */
    @Transactional(readOnly = true)
    public PageResult<OperationLogDto> myLogs(Long operatorId, int page, int size) {
        Page<OperationLog> result = operationLogRepository.findByOperatorId(
                operatorId, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(operationLogMapper::toDto));
    }
}
