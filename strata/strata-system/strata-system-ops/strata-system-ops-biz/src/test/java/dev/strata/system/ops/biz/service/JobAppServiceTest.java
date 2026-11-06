// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.JobDto;
import dev.strata.system.api.request.JobCreateRequest;
import dev.strata.system.api.request.JobUpdateRequest;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import dev.strata.system.ops.api.entity.SysJob;
import dev.strata.system.ops.biz.mapper.JobMapper;
import dev.strata.system.ops.biz.repository.ScheduleTaskLogRepository;
import dev.strata.system.ops.biz.repository.SysJobRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAppService 单元测试")
class JobAppServiceTest {

    @Mock
    private SysJobRepository sysJobRepository;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private ScheduleTaskLogRepository scheduleTaskLogRepository;

    private CommunityFeaturesProperties communityFeatures;

    private JobAppService jobAppService;

    @BeforeEach
    void setUp() {
        communityFeatures = new CommunityFeaturesProperties();
        jobAppService = new JobAppService(sysJobRepository, jobMapper, communityFeatures, scheduleTaskLogRepository);
    }

    private SysJob job(long id, String group) {
        SysJob j = new SysJob();
        j.setId(id);
        j.setName("report-job");
        j.setJobGroup(group);
        j.setCron("0 0 * * * ?");
        j.setStatus("PAUSED");
        j.setEnabled(true);
        return j;
    }

    private JobDto dto() {
        return new JobDto("1", "report-job", "DEFAULT", "0 0 * * * ?", "PAUSED", null, null, true);
    }

    @Nested
    @DisplayName("list / page / get - 查询")
    class QueryTests {

        @Test
        @DisplayName("list 返回全部任务")
        void list_MapsAll() {
            when(sysJobRepository.findAllByOrderByIdAsc()).thenReturn(List.of(job(1L, "DEFAULT")));
            when(jobMapper.toDto(any(SysJob.class))).thenReturn(dto());

            List<JobDto> result = jobAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("page 分页查询")
        void page_ReturnsPaged() {
            when(sysJobRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(job(1L, "DEFAULT"))));
            when(jobMapper.toDto(any(SysJob.class))).thenReturn(dto());

            PageResult<JobDto> result = jobAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("get 存在时返回 DTO")
        void get_Found() {
            when(sysJobRepository.findById(1L)).thenReturn(Optional.of(job(1L, "DEFAULT")));
            when(jobMapper.toDto(any(SysJob.class))).thenReturn(dto());

            JobDto result = jobAppService.get(1L);

            assertThat(result.name()).isEqualTo("report-job");
        }

        @Test
        @DisplayName("get 不存在抛出 NOT_FOUND")
        void get_NotFound_Throws() {
            when(sysJobRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("create - 创建任务")
    class CreateTests {

        @Test
        @DisplayName("jobGroup 为空白时默认 DEFAULT")
        void create_BlankGroup_DefaultsToDefault() {
            JobCreateRequest request = new JobCreateRequest("j", "  ", "0 0 * * * ?", true);
            SysJob entity = job(0L, "  ");
            when(jobMapper.toEntity(request)).thenReturn(entity);
            when(sysJobRepository.save(entity)).thenReturn(entity);
            when(jobMapper.toDto(entity)).thenReturn(dto());

            jobAppService.create(request);

            assertThat(entity.getJobGroup()).isEqualTo("DEFAULT");
            verify(sysJobRepository).save(entity);
        }

        @Test
        @DisplayName("jobGroup 非空时保留")
        void create_ExplicitGroup_Kept() {
            JobCreateRequest request = new JobCreateRequest("j", "CRON", "0 0 * * * ?", true);
            SysJob entity = job(0L, "CRON");
            when(jobMapper.toEntity(request)).thenReturn(entity);
            when(sysJobRepository.save(entity)).thenReturn(entity);
            when(jobMapper.toDto(entity)).thenReturn(dto());

            jobAppService.create(request);

            assertThat(entity.getJobGroup()).isEqualTo("CRON");
        }
    }

    @Nested
    @DisplayName("update / delete / pause / run")
    class MutationTests {

        @Test
        @DisplayName("更新任务成功")
        void update_Success() {
            SysJob existing = job(1L, "DEFAULT");
            JobUpdateRequest request = new JobUpdateRequest("renamed", "G", "0 1 * * * ?", false);
            when(sysJobRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(sysJobRepository.save(existing)).thenReturn(existing);
            when(jobMapper.toDto(existing)).thenReturn(dto());

            jobAppService.update(1L, request);

            verify(jobMapper).update(eq(request), any(SysJob.class));
            verify(sysJobRepository).save(existing);
        }

        @Test
        @DisplayName("删除任务成功")
        void delete_Success() {
            when(sysJobRepository.findById(1L)).thenReturn(Optional.of(job(1L, "DEFAULT")));

            jobAppService.delete(1L);

            verify(sysJobRepository).delete(any(SysJob.class));
        }

        @Test
        @DisplayName("暂停任务置为 PAUSED 并禁用")
        void pause_MarksPausedAndDisabled() {
            SysJob existing = job(1L, "DEFAULT");
            when(sysJobRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(sysJobRepository.save(existing)).thenReturn(existing);
            when(jobMapper.toDto(existing)).thenReturn(dto());

            jobAppService.pause(1L);

            assertThat(existing.getStatus()).isEqualTo("PAUSED");
            assertThat(existing.isEnabled()).isFalse();
            verify(sysJobRepository).save(existing);
        }

        @Test
        @DisplayName("toggle 翻转 enabled")
        void toggle_FlipsEnabled() {
            SysJob existing = job(1L, "DEFAULT");
            existing.setEnabled(true);
            when(sysJobRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(sysJobRepository.save(existing)).thenReturn(existing);
            when(jobMapper.toDto(existing)).thenReturn(dto());

            jobAppService.toggle(1L);

            assertThat(existing.isEnabled()).isFalse();
            verify(sysJobRepository).save(existing);
        }

        @Test
        @DisplayName("stub 模式下 run 模拟一次 tick 后回到 PAUSED")
        void run_StubMode_SimulatesTick() {
            communityFeatures.setJobSchedulerStub(true);
            SysJob existing = job(1L, "DEFAULT");
            when(sysJobRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(sysJobRepository.save(existing)).thenReturn(existing);
            when(jobMapper.toDto(existing)).thenReturn(dto());

            jobAppService.run(1L);

            assertThat(existing.getStatus()).isEqualTo("PAUSED");
            assertThat(existing.getLastRun()).isNotNull();
            assertThat(existing.getNextRun()).isAfter(existing.getLastRun());
            verify(sysJobRepository, times(2)).save(existing);
            verify(scheduleTaskLogRepository).save(any(dev.strata.system.ops.api.entity.ScheduleTaskLog.class));
        }

        @Test
        @DisplayName("非 stub 模式 run 拒绝执行")
        void run_RealSchedulerUnavailable_Throws() {
            communityFeatures.setJobSchedulerStub(false);

            assertThatThrownBy(() -> jobAppService.run(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

            verify(sysJobRepository, never()).save(any(SysJob.class));
        }
    }
}
