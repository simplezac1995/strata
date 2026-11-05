// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.ParamDto;
import dev.strata.system.api.request.ParamCreateRequest;
import dev.strata.system.api.request.ParamUpdateRequest;
import dev.strata.system.config.api.entity.SysParam;
import dev.strata.system.config.biz.mapper.ParamMapper;
import dev.strata.system.config.biz.metrics.ConfigMetrics;
import dev.strata.system.config.biz.repository.SysParamRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParamAppService {

    private static final Logger log = LoggerFactory.getLogger(ParamAppService.class);

    private static final String DEFAULT_TYPE = "string";

    private final SysParamRepository sysParamRepository;
    private final ParamMapper paramMapper;
    private final ConfigMetrics configMetrics;

    public ParamAppService(
            SysParamRepository sysParamRepository, ParamMapper paramMapper, ConfigMetrics configMetrics) {
        this.sysParamRepository = sysParamRepository;
        this.paramMapper = paramMapper;
        this.configMetrics = configMetrics;
    }

    @Transactional(readOnly = true)
    public List<ParamDto> list() {
        return sysParamRepository.findAllByOrderByIdAsc().stream()
                .map(paramMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<ParamDto> page(String keyword, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var result = keyword == null || keyword.isBlank()
                ? sysParamRepository.findAll(pageable)
                : sysParamRepository.findByKeyContainingIgnoreCase(keyword.trim(), pageable);
        return PageResult.from(result.map(paramMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "system:param", action = "create")
    public ParamDto create(ParamCreateRequest request) {
        if (sysParamRepository.existsByKey(request.key())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Param key already exists");
        }
        SysParam param = paramMapper.toEntity(request);
        if (param.getType() == null || param.getType().isBlank()) {
            param.setType(DEFAULT_TYPE);
        }
        sysParamRepository.save(param);
        return paramMapper.toDto(param);
    }

    @Transactional
    @OperationLog(module = "system:param", action = "update")
    public ParamDto update(Long id, ParamUpdateRequest request) {
        SysParam param = require(id);
        paramMapper.update(request, param);
        sysParamRepository.save(param);
        configMetrics.recordParamUpdated();
        return paramMapper.toDto(param);
    }

    @Transactional
    @OperationLog(module = "system:param", action = "delete")
    public void delete(Long id) {
        sysParamRepository.delete(require(id));
    }

    private SysParam require(Long id) {
        return sysParamRepository.findById(id).orElseThrow(() -> new NotFoundException("Param not found"));
    }
}
