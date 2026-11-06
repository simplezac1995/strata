// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.system.api.dto.PopupConfigDto;
import dev.strata.system.api.request.PopupUpsertRequest;
import dev.strata.system.ops.api.entity.PopupConfig;
import dev.strata.system.ops.biz.mapper.PopupConfigMapper;
import dev.strata.system.ops.biz.repository.PopupConfigRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PopupConfigAppService {

    private final PopupConfigRepository popupConfigRepository;
    private final PopupConfigMapper popupConfigMapper;

    public PopupConfigAppService(PopupConfigRepository popupConfigRepository, PopupConfigMapper popupConfigMapper) {
        this.popupConfigRepository = popupConfigRepository;
        this.popupConfigMapper = popupConfigMapper;
    }

    @Transactional(readOnly = true)
    public List<PopupConfigDto> list() {
        return popupConfigRepository.findAllByOrderByIdAsc().stream()
                .map(popupConfigMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PopupConfigDto get(Long id) {
        return popupConfigMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ops:popup", action = "create")
    public PopupConfigDto create(PopupUpsertRequest request) {
        PopupConfig popup = popupConfigMapper.toEntity(request);
        popupConfigRepository.save(popup);
        return popupConfigMapper.toDto(popup);
    }

    @Transactional
    @OperationLog(module = "ops:popup", action = "update")
    public PopupConfigDto update(Long id, PopupUpsertRequest request) {
        PopupConfig popup = require(id);
        popupConfigMapper.update(request, popup);
        popupConfigRepository.save(popup);
        return popupConfigMapper.toDto(popup);
    }

    @Transactional
    @OperationLog(module = "ops:popup", action = "delete")
    public void delete(Long id) {
        popupConfigRepository.delete(require(id));
    }

    private PopupConfig require(Long id) {
        return popupConfigRepository.findById(id).orElseThrow(() -> new NotFoundException("Popup config not found"));
    }
}
