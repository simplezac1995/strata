// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.JobDto;
import dev.strata.system.api.request.JobCreateRequest;
import dev.strata.system.api.request.JobUpdateRequest;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import dev.strata.system.ops.api.entity.ScheduleTaskLog;
import dev.strata.system.ops.api.entity.SysJob;
import dev.strata.system.ops.biz.mapper.JobMapper;
import dev.strata.system.ops.biz.repository.ScheduleTaskLogRepository;
import dev.strata.system.ops.biz.repository.SysJobRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobAppService {

    private static final Logger log = LoggerFactory.getLogger(JobAppService.class);

    private final SysJobRepository sysJobRepository;
    private final JobMapper jobMapper;
    private final CommunityFeaturesProperties communityFeatures;
    private final ScheduleTaskLogRepository scheduleTaskLogRepository;

    public JobAppService(
            SysJobRepository sysJobRepository,
            JobMapper jobMapper,
            CommunityFeaturesProperties communityFeatures,
            ScheduleTaskLogRepository scheduleTaskLogRepository) {
        this.sysJobRepository = sysJobRepository;
        this.jobMapper = jobMapper;
        this.communityFeatures = communityFeatures;
        this.scheduleTaskLogRepository = scheduleTaskLogRepository;
    }

    @Transactional(readOnly = true)
    public List<JobDto> list() {
        return sysJobRepository.findAllByOrderByIdAsc().stream()
                .map(jobMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<JobDto> page(int page, int size) {
        return PageResult.from(sysJobRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(jobMapper::toDto));
    }

    @Transactional(readOnly = true)
    public JobDto get(Long id) {
        return jobMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "system:job", action = "create")
    public JobDto create(JobCreateRequest request) {
        SysJob job = jobMapper.toEntity(request);
        if (job.getJobGroup() == null || job.getJobGroup().isBlank()) {
            job.setJobGroup("DEFAULT");
        }
        sysJobRepository.save(job);
        return jobMapper.toDto(job);
    }

    @Transactional
    @OperationLog(module = "system:job", action = "update")
    public JobDto update(Long id, JobUpdateRequest request) {
        SysJob job = require(id);
        jobMapper.update(request, job);
        sysJobRepository.save(job);
        return jobMapper.toDto(job);
    }

    @Transactional
    @OperationLog(module = "system:job", action = "delete")
    public void delete(Long id) {
        sysJobRepository.delete(require(id));
    }

    @Transactional
    @OperationLog(module = "system:job", action = "update")
    public JobDto pause(Long id) {
        SysJob job = require(id);
        job.setStatus("PAUSED");
        job.setEnabled(false);
        sysJobRepository.save(job);
        return jobMapper.toDto(job);
    }

    /** Toggle between paused (disabled) and resumed (enabled). */
    @Transactional
    @OperationLog(module = "system:job", action = "update")
    public JobDto toggle(Long id) {
        SysJob job = require(id);
        job.setEnabled(!job.isEnabled());
        job.setStatus("PAUSED");
        sysJobRepository.save(job);
        return jobMapper.toDto(job);
    }

    @Transactional
    @OperationLog(module = "system:job", action = "update")
    public JobDto run(Long id) {
        if (!communityFeatures.isJobSchedulerStub()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST, "Real job scheduler is not available in Community edition");
        }
        SysJob job = require(id);
        Instant start = Instant.now();
        String status = "SUCCESS";
        String message = "completed";
        try {
            Instant now = Instant.now();
            job.setStatus("RUNNING");
            job.setEnabled(true);
            job.setLastRun(now);
            job.setNextRun(now.plusSeconds(3600));
            sysJobRepository.save(job);
            /* Simulate a completed tick for the admin UI (no real scheduler in MVP). */
            job.setStatus("PAUSED");
            sysJobRepository.save(job);
            return jobMapper.toDto(job);
        } catch (RuntimeException e) {
            status = "FAILURE";
            message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw e;
        } finally {
            recordRun(job, start, status, message);
        }
    }

    private void recordRun(SysJob job, Instant start, String status, String message) {
        try {
            Instant end = Instant.now();
            ScheduleTaskLog logRow = new ScheduleTaskLog();
            logRow.setJobId(job.getId());
            logRow.setJobName(job.getName());
            logRow.setStatus(status);
            logRow.setStartTime(start);
            logRow.setEndTime(end);
            logRow.setDurationMs(java.time.Duration.between(start, end).toMillis());
            logRow.setMessage(message);
            scheduleTaskLogRepository.save(logRow);
        } catch (RuntimeException e) {
            log.warn("Failed to record schedule task log for job {}: {}", job.getId(), e.getMessage());
        }
    }

    private SysJob require(Long id) {
        return sysJobRepository.findById(id).orElseThrow(() -> new NotFoundException("Job not found"));
    }
}
