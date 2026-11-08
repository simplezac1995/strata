// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.system.api.dto.DictDataDto;
import dev.strata.system.api.request.DictDataCreateRequest;
import dev.strata.system.api.request.DictDataUpdateRequest;
import dev.strata.system.config.api.entity.DictData;
import dev.strata.system.config.biz.mapper.DictDataMapper;
import dev.strata.system.config.biz.metrics.ConfigMetrics;
import dev.strata.system.config.biz.repository.DictDataRepository;
import dev.strata.system.config.biz.repository.DictTypeRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictDataAppService {

    private static final Logger log = LoggerFactory.getLogger(DictDataAppService.class);

    private final DictDataRepository dictDataRepository;
    private final DictTypeRepository dictTypeRepository;
    private final DictDataMapper dictDataMapper;
    private final ConfigMetrics configMetrics;

    public DictDataAppService(
            DictDataRepository dictDataRepository,
            DictTypeRepository dictTypeRepository,
            DictDataMapper dictDataMapper,
            ConfigMetrics configMetrics) {
        this.dictDataRepository = dictDataRepository;
        this.dictTypeRepository = dictTypeRepository;
        this.dictDataMapper = dictDataMapper;
        this.configMetrics = configMetrics;
    }

    @Transactional(readOnly = true)
    public List<DictDataDto> listByType(Long typeId) {
        requireType(typeId);
        return dictDataRepository.findAllByTypeIdOrderBySortOrderAscIdAsc(typeId).stream()
                .map(dictDataMapper::toDto)
                .toList();
    }

    @Transactional
    @OperationLog(module = "system:dict", action = "create")
    public DictDataDto create(DictDataCreateRequest request) {
        requireType(request.typeId());
        if (dictDataRepository.existsByTypeIdAndValue(request.typeId(), request.value())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Dict value already exists in this type");
        }
        DictData data = dictDataMapper.toEntity(request);
        dictDataRepository.save(data);
        configMetrics.recordDictDataCreated();
        return dictDataMapper.toDto(data);
    }

    @Transactional
    @OperationLog(module = "system:dict", action = "update")
    public DictDataDto update(Long id, DictDataUpdateRequest request) {
        DictData data = require(id);
        dictDataMapper.update(request, data);
        dictDataRepository.save(data);
        configMetrics.recordDictDataUpdated();
        return dictDataMapper.toDto(data);
    }

    @Transactional
    @OperationLog(module = "system:dict", action = "delete")
    public void delete(Long id) {
        dictDataRepository.delete(require(id));
        configMetrics.recordDictDataDeleted();
    }

    private void requireType(Long typeId) {
        if (!dictTypeRepository.existsById(typeId)) {
            throw new NotFoundException("Dict type not found");
        }
    }

    private DictData require(Long id) {
        return dictDataRepository.findById(id).orElseThrow(() -> new NotFoundException("Dict data not found"));
    }
}
