// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.dto.DailyTaskDto;
import dev.strata.system.api.request.DailyTaskUpsertRequest;
import dev.strata.system.ops.api.entity.DailyTask;
import dev.strata.system.ops.biz.mapper.DailyTaskMapper;
import dev.strata.system.ops.biz.repository.DailyTaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyTaskAppService 单元测试")
class DailyTaskAppServiceTest {

    @Mock
    private DailyTaskRepository dailyTaskRepository;

    @Mock
    private DailyTaskMapper dailyTaskMapper;

    private DailyTaskAppService service;

    @BeforeEach
    void setUp() {
        service = new DailyTaskAppService(dailyTaskRepository, dailyTaskMapper);
    }

    private DailyTask task() {
        DailyTask t = new DailyTask();
        t.setName("login");
        return t;
    }

    private DailyTaskDto dto() {
        return new DailyTaskDto("1", "login", "login", 1, 100L);
    }

    @Test
    @DisplayName("list 返回全部")
    void list_MapsAll() {
        when(dailyTaskRepository.findAllByOrderByIdAsc()).thenReturn(List.of(task()));
        when(dailyTaskMapper.toDto(any(DailyTask.class))).thenReturn(dto());

        assertThat(service.list()).hasSize(1);
    }

    @Test
    @DisplayName("get 存在返回 DTO")
    void get_Found() {
        when(dailyTaskRepository.findById(1L)).thenReturn(Optional.of(task()));
        when(dailyTaskMapper.toDto(any(DailyTask.class))).thenReturn(dto());

        assertThat(service.get(1L).name()).isEqualTo("login");
    }

    @Test
    @DisplayName("get 不存在抛 NOT_FOUND")
    void get_NotFound_Throws() {
        when(dailyTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("create 保存")
    void create_Success() {
        DailyTaskUpsertRequest request = new DailyTaskUpsertRequest("login", "login", 1, 100L);
        when(dailyTaskMapper.toEntity(request)).thenReturn(task());
        when(dailyTaskRepository.save(any(DailyTask.class))).thenReturn(task());
        when(dailyTaskMapper.toDto(any(DailyTask.class))).thenReturn(dto());

        service.create(request);

        verify(dailyTaskRepository).save(any(DailyTask.class));
    }

    @Test
    @DisplayName("update 更新")
    void update_Success() {
        DailyTaskUpsertRequest request = new DailyTaskUpsertRequest("bet", "bet", 10, 50L);
        when(dailyTaskRepository.findById(1L)).thenReturn(Optional.of(task()));
        when(dailyTaskRepository.save(any(DailyTask.class))).thenReturn(task());
        when(dailyTaskMapper.toDto(any(DailyTask.class))).thenReturn(dto());

        service.update(1L, request);

        verify(dailyTaskMapper).update(eq(request), any(DailyTask.class));
    }

    @Test
    @DisplayName("delete 删除")
    void delete_Success() {
        when(dailyTaskRepository.findById(1L)).thenReturn(Optional.of(task()));

        service.delete(1L);

        verify(dailyTaskRepository).delete(any(DailyTask.class));
    }
}
