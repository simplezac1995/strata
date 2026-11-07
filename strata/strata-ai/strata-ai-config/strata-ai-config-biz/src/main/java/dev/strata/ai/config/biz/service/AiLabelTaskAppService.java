// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiLabelSampleCreateRequest;
import dev.strata.ai.api.request.AiLabelTaskCreateRequest;
import dev.strata.ai.api.request.AiLabelTaskUpdateRequest;
import dev.strata.ai.config.api.dto.AiLabelSampleDto;
import dev.strata.ai.config.api.dto.AiLabelTaskDto;
import dev.strata.ai.config.api.entity.AiLabelSample;
import dev.strata.ai.config.api.entity.AiLabelTask;
import dev.strata.ai.config.biz.mapper.AiLabelSampleMapper;
import dev.strata.ai.config.biz.mapper.AiLabelTaskMapper;
import dev.strata.ai.config.biz.repository.AiLabelSampleRepository;
import dev.strata.ai.config.biz.repository.AiLabelTaskRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thin CRUD over {@code ai_label_task} / {@code ai_label_sample}. Tasks are soft-deleted; adding a sample
 * bumps the owning task's {@code sample_count} so the list page never has to count.
 */
@Service
public class AiLabelTaskAppService {

    private static final Logger log = LoggerFactory.getLogger(AiLabelTaskAppService.class);

    private static final Set<String> TASK_TYPES = Set.of("text", "image", "audio");
    private static final Set<String> TASK_STATUSES = Set.of("pending", "in_progress", "completed");

    private final AiLabelTaskRepository aiLabelTaskRepository;
    private final AiLabelSampleRepository aiLabelSampleRepository;
    private final AiLabelTaskMapper aiLabelTaskMapper;
    private final AiLabelSampleMapper aiLabelSampleMapper;

    public AiLabelTaskAppService(
            AiLabelTaskRepository aiLabelTaskRepository,
            AiLabelSampleRepository aiLabelSampleRepository,
            AiLabelTaskMapper aiLabelTaskMapper,
            AiLabelSampleMapper aiLabelSampleMapper) {
        this.aiLabelTaskRepository = aiLabelTaskRepository;
        this.aiLabelSampleRepository = aiLabelSampleRepository;
        this.aiLabelTaskMapper = aiLabelTaskMapper;
        this.aiLabelSampleMapper = aiLabelSampleMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<AiLabelTaskDto> pageTasks(int page, int size) {
        return PageResult.from(aiLabelTaskRepository
                .findByDeletedFalse(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(aiLabelTaskMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResult<AiLabelSampleDto> pageSamples(Long taskId, int page, int size) {
        requireTask(taskId);
        return PageResult.from(aiLabelSampleRepository
                .findByTaskId(taskId, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(aiLabelSampleMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "ai:label", action = "create")
    public AiLabelTaskDto createTask(AiLabelTaskCreateRequest request) {
        requireType(request.type());
        AiLabelTask task = aiLabelTaskMapper.toEntity(request);
        task.setSampleCount(0);
        task.setStatus("pending");
        aiLabelTaskRepository.save(task);
        return aiLabelTaskMapper.toDto(task);
    }

    @Transactional
    @OperationLog(module = "ai:label", action = "update")
    public AiLabelTaskDto updateTask(Long id, AiLabelTaskUpdateRequest request) {
        AiLabelTask task = requireTask(id);
        if (request.type() != null) {
            requireType(request.type());
        }
        if (request.status() != null) {
            requireStatus(request.status());
        }
        aiLabelTaskMapper.update(request, task);
        aiLabelTaskRepository.save(task);
        return aiLabelTaskMapper.toDto(task);
    }

    @Transactional
    @OperationLog(module = "ai:label", action = "delete")
    public void deleteTask(Long id) {
        AiLabelTask task = requireTask(id);
        task.setDeleted(true);
        aiLabelTaskRepository.save(task);
    }

    @Transactional
    @OperationLog(module = "ai:label", action = "create")
    public AiLabelSampleDto addSample(Long taskId, AiLabelSampleCreateRequest request) {
        AiLabelTask task = requireTask(taskId);
        AiLabelSample sample = aiLabelSampleMapper.toEntity(request);
        sample.setTaskId(taskId);
        sample.setSource("manual");
        aiLabelSampleRepository.save(sample);
        task.setSampleCount((int) aiLabelSampleRepository.countByTaskId(taskId));
        aiLabelTaskRepository.save(task);
        return aiLabelSampleMapper.toDto(sample);
    }

    private void requireType(String type) {
        if (!TASK_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported label task type: " + type);
        }
    }

    private void requireStatus(String status) {
        if (!TASK_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported label task status: " + status);
        }
    }

    private AiLabelTask requireTask(Long id) {
        return aiLabelTaskRepository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Ai label task not found"));
    }
}
