// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.system.api.dto.DailyTaskDto;
import dev.strata.system.api.request.DailyTaskUpsertRequest;
import dev.strata.system.ops.api.entity.DailyTask;
import dev.strata.system.ops.biz.mapper.DailyTaskMapper;
import dev.strata.system.ops.biz.repository.DailyTaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyTaskAppService {

    private final DailyTaskRepository dailyTaskRepository;
    private final DailyTaskMapper dailyTaskMapper;

    public DailyTaskAppService(DailyTaskRepository dailyTaskRepository, DailyTaskMapper dailyTaskMapper) {
        this.dailyTaskRepository = dailyTaskRepository;
        this.dailyTaskMapper = dailyTaskMapper;
    }

    @Transactional(readOnly = true)
    public List<DailyTaskDto> list() {
        return dailyTaskRepository.findAllByOrderByIdAsc().stream()
                .map(dailyTaskMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyTaskDto get(Long id) {
        return dailyTaskMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ops:daily-task", action = "create")
    public DailyTaskDto create(DailyTaskUpsertRequest request) {
        DailyTask task = dailyTaskMapper.toEntity(request);
        dailyTaskRepository.save(task);
        return dailyTaskMapper.toDto(task);
    }

    @Transactional
    @OperationLog(module = "ops:daily-task", action = "update")
    public DailyTaskDto update(Long id, DailyTaskUpsertRequest request) {
        DailyTask task = require(id);
        dailyTaskMapper.update(request, task);
        dailyTaskRepository.save(task);
        return dailyTaskMapper.toDto(task);
    }

    @Transactional
    @OperationLog(module = "ops:daily-task", action = "delete")
    public void delete(Long id) {
        dailyTaskRepository.delete(require(id));
    }

    private DailyTask require(Long id) {
        return dailyTaskRepository.findById(id).orElseThrow(() -> new NotFoundException("Daily task not found"));
    }
}
