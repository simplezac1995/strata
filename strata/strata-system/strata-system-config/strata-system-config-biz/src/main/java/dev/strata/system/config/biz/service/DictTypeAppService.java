// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.system.api.dto.DictTypeDto;
import dev.strata.system.api.request.DictTypeCreateRequest;
import dev.strata.system.api.request.DictTypeUpdateRequest;
import dev.strata.system.config.api.entity.DictType;
import dev.strata.system.config.biz.mapper.DictTypeMapper;
import dev.strata.system.config.biz.metrics.ConfigMetrics;
import dev.strata.system.config.biz.repository.DictDataRepository;
import dev.strata.system.config.biz.repository.DictTypeRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictTypeAppService {

    private static final Logger log = LoggerFactory.getLogger(DictTypeAppService.class);

    private final DictTypeRepository dictTypeRepository;
    private final DictDataRepository dictDataRepository;
    private final DictTypeMapper dictTypeMapper;
    private final ConfigMetrics configMetrics;

    public DictTypeAppService(
            DictTypeRepository dictTypeRepository,
            DictDataRepository dictDataRepository,
            DictTypeMapper dictTypeMapper,
            ConfigMetrics configMetrics) {
        this.dictTypeRepository = dictTypeRepository;
        this.dictDataRepository = dictDataRepository;
        this.dictTypeMapper = dictTypeMapper;
        this.configMetrics = configMetrics;
    }

    @Transactional(readOnly = true)
    public List<DictTypeDto> list() {
        return dictTypeRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(dictTypeMapper::toDto)
                .toList();
    }

    @Transactional
    @OperationLog(module = "system:dict", action = "create")
    public DictTypeDto create(DictTypeCreateRequest request) {
        if (dictTypeRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Dict type code already exists");
        }
        DictType type = dictTypeMapper.toEntity(request);
        dictTypeRepository.save(type);
        configMetrics.recordDictTypeCreated();
        return dictTypeMapper.toDto(type);
    }

    @Transactional
    @OperationLog(module = "system:dict", action = "update")
    public DictTypeDto update(Long id, DictTypeUpdateRequest request) {
        DictType type = require(id);
        dictTypeMapper.update(request, type);
        dictTypeRepository.save(type);
        configMetrics.recordDictTypeUpdated();
        return dictTypeMapper.toDto(type);
    }

    @Transactional
    @OperationLog(module = "system:dict", action = "delete")
    public void delete(Long id) {
        require(id);
        dictDataRepository.deleteByTypeId(id);
        dictTypeRepository.deleteById(id);
        configMetrics.recordDictTypeDeleted();
    }

    private DictType require(Long id) {
        return dictTypeRepository.findById(id).orElseThrow(() -> new NotFoundException("Dict type not found"));
    }
}
