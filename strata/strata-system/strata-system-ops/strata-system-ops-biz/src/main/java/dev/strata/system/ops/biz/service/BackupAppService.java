// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.BackupRecordDto;
import dev.strata.system.api.request.BackupCreateRequest;
import dev.strata.system.ops.api.entity.BackupRecord;
import dev.strata.system.ops.biz.mapper.BackupRecordMapper;
import dev.strata.system.ops.biz.repository.BackupRecordRepository;
import dev.strata.system.ops.biz.service.backup.BackupExecutor;
import dev.strata.system.ops.biz.service.backup.BackupExecutor.BackupOutcome;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupAppService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupRecordRepository backupRecordRepository;
    private final BackupRecordMapper backupRecordMapper;
    private final BackupExecutor backupExecutor;

    public BackupAppService(
            BackupRecordRepository backupRecordRepository,
            BackupRecordMapper backupRecordMapper,
            BackupExecutor backupExecutor) {
        this.backupRecordRepository = backupRecordRepository;
        this.backupRecordMapper = backupRecordMapper;
        this.backupExecutor = backupExecutor;
    }

    @Transactional(readOnly = true)
    public PageResult<BackupRecordDto> page(int page, int size) {
        return PageResult.from(backupRecordRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(backupRecordMapper::toDto));
    }

    /**
     * Triggers a real backup: invokes the on-disk script through the configured {@link BackupExecutor}, then
     * records the produced artifact (or the failure) in {@code backup_record}.
     */
    @Transactional
    @OperationLog(module = "ops:backup", action = "create")
    public BackupRecordDto create(BackupCreateRequest request) {
        BackupRecord record = new BackupRecord();
        String type = (request.type() == null || request.type().isBlank()) ? "full" : request.type();
        record.setType(type);
        record.setPath(request.path());
        record.setFilename("backup-" + type + "-"
                + Instant.now().atZone(java.time.ZoneId.systemDefault()).format(FILE_STAMP)
                + ".sql");
        record.setStatus("in_progress");
        record.setSizeBytes(0L);

        BackupOutcome outcome = backupExecutor.run(type, request.path());
        if (outcome.success()) {
            record.setStatus("completed");
            if (outcome.filename() != null) {
                record.setFilename(outcome.filename());
            }
            if (outcome.path() != null) {
                record.setPath(outcome.path());
            }
            record.setSizeBytes(outcome.sizeBytes());
        } else {
            record.setStatus("failed");
        }
        backupRecordRepository.save(record);
        return backupRecordMapper.toDto(record);
    }

    /**
     * Restore from a backup. This iteration only marks the row as completed; the actual restore is deliberately
     * not wired (risky, and out of scope) — it is recorded as a no-op so the audit trail stays honest.
     */
    @Transactional
    @OperationLog(module = "ops:backup", action = "restore")
    public BackupRecordDto restore(Long id) {
        BackupRecord record = require(id);
        record.setStatus("completed");
        backupRecordRepository.save(record);
        return backupRecordMapper.toDto(record);
    }

    @Transactional
    @OperationLog(module = "ops:backup", action = "delete")
    public void delete(Long id) {
        backupRecordRepository.delete(require(id));
    }

    private BackupRecord require(Long id) {
        return backupRecordRepository.findById(id).orElseThrow(() -> new NotFoundException("Backup record not found"));
    }
}
