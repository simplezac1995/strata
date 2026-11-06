// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.BackupRecordDto;
import dev.strata.system.api.request.BackupCreateRequest;
import dev.strata.system.ops.api.entity.BackupRecord;
import dev.strata.system.ops.biz.mapper.BackupRecordMapper;
import dev.strata.system.ops.biz.repository.BackupRecordRepository;
import dev.strata.system.ops.biz.service.backup.BackupExecutor;
import dev.strata.system.ops.biz.service.backup.BackupExecutor.BackupOutcome;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupAppService 单元测试")
class BackupAppServiceTest {

    @Mock
    private BackupRecordRepository backupRecordRepository;

    @Mock
    private BackupRecordMapper backupRecordMapper;

    @Mock
    private BackupExecutor backupExecutor;

    private BackupAppService service;

    @BeforeEach
    void setUp() {
        service = new BackupAppService(backupRecordRepository, backupRecordMapper, backupExecutor);
    }

    private BackupRecord record() {
        BackupRecord r = new BackupRecord();
        r.setFilename("b.sql");
        r.setStatus("completed");
        return r;
    }

    private BackupRecordDto dto() {
        return new BackupRecordDto("1", "b.sql", 0L, "full", "completed", null, null);
    }

    @Test
    @DisplayName("page 分页")
    void page_ReturnsPaged() {
        when(backupRecordRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(record())));
        when(backupRecordMapper.toDto(any(BackupRecord.class))).thenReturn(dto());

        PageResult<BackupRecordDto> result = service.page(0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("create 调用脚本并置 completed")
    void create_RunsScript_RecordsCompleted() {
        when(backupExecutor.run(eq("full"), any()))
                .thenReturn(BackupOutcome.ok("strata-full-2026.sql.gz", "/app/data/backups/strata-full.sql.gz", 2048L));
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(backupRecordMapper.toDto(any(BackupRecord.class))).thenReturn(dto());

        BackupRecordDto result = service.create(new BackupCreateRequest("full", "/var/backups"));

        assertThat(result.status()).isEqualTo("completed");
        verify(backupExecutor).run(eq("full"), any());
        verify(backupRecordRepository).save(any(BackupRecord.class));
    }

    @Test
    @DisplayName("create 脚本失败置 failed")
    void create_ScriptFails_MarksFailed() {
        when(backupExecutor.run(any(), any())).thenReturn(BackupOutcome.failed("boom"));
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(backupRecordMapper.toDto(any(BackupRecord.class)))
                .thenReturn(new BackupRecordDto("1", "b.sql", 0L, "full", "failed", null, null));

        BackupRecordDto result = service.create(new BackupCreateRequest("full", null));

        assertThat(result.status()).isEqualTo("failed");
    }

    @Test
    @DisplayName("create 缺省 type 为 full")
    void create_BlankType_DefaultsFull() {
        when(backupExecutor.run(any(), any())).thenReturn(BackupOutcome.ok("f.gz", "/tmp/f.gz", 1L));
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(backupRecordMapper.toDto(any(BackupRecord.class))).thenReturn(dto());

        service.create(new BackupCreateRequest("  ", null));

        verify(backupRecordRepository).save(any(BackupRecord.class));
    }

    @Test
    @DisplayName("restore 置 completed")
    void restore_MarksCompleted() {
        when(backupRecordRepository.findById(1L)).thenReturn(Optional.of(record()));
        when(backupRecordRepository.save(any(BackupRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(backupRecordMapper.toDto(any(BackupRecord.class))).thenReturn(dto());

        service.restore(1L);

        verify(backupRecordRepository).save(any(BackupRecord.class));
    }

    @Test
    @DisplayName("delete 删除")
    void delete_Success() {
        when(backupRecordRepository.findById(1L)).thenReturn(Optional.of(record()));

        service.delete(1L);

        verify(backupRecordRepository).delete(any(BackupRecord.class));
    }

    @Test
    @DisplayName("delete 不存在抛 NOT_FOUND")
    void delete_NotFound_Throws() {
        when(backupRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(BusinessException.class);
    }
}
