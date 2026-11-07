// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.AuditLogDto;
import dev.strata.system.api.event.AuditLogEvent;
import dev.strata.system.ops.api.entity.AuditLog;
import dev.strata.system.ops.biz.mapper.AuditLogMapper;
import dev.strata.system.ops.biz.repository.AuditLogRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogAppService 单元测试")
class AuditLogAppServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogAppService auditLogAppService;

    @Test
    @DisplayName("分页查询审计日志并按 id 倒序映射 DTO")
    void page_ReturnsPagedDtos() {
        AuditLog log = new AuditLog();
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log)));
        when(auditLogMapper.toDto(any(AuditLog.class)))
                .thenReturn(new AuditLogDto(
                        "1",
                        "CREATE",
                        "system:dept",
                        "create",
                        "Dept",
                        "5",
                        "admin",
                        "127.0.0.1",
                        true,
                        null,
                        null,
                        Instant.now()));

        PageResult<AuditLogDto> result = auditLogAppService.page(0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).eventType()).isEqualTo("CREATE");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("空页返回空内容")
    void page_Empty_ReturnsEmpty() {
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResult<AuditLogDto> result = auditLogAppService.page(1, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    @DisplayName("失败与成功操作审计事件独立写入并完整保留安全字段")
    void recordEvent_PersistsAuditEventInNewTransaction() throws NoSuchMethodException {
        AuditLogEvent event = new AuditLogEvent(
                "DATA",
                "compliance:aml",
                "review",
                "aml",
                null,
                42L,
                "reviewer",
                "192.0.2.4",
                "agent",
                false,
                "AmlAppService.review(..) failed (IllegalStateException)");

        auditLogAppService.recordEvent(event);

        org.mockito.ArgumentCaptor<AuditLog> savedLog = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(savedLog.capture());
        assertThat(savedLog.getValue().getModuleName()).isEqualTo("compliance:aml");
        assertThat(savedLog.getValue().getActionName()).isEqualTo("review");
        assertThat(savedLog.getValue().getOperatorId()).isEqualTo(42L);
        assertThat(savedLog.getValue().isSuccess()).isFalse();
        assertThat(savedLog.getValue().getDetail()).contains("IllegalStateException");

        Method method = AuditLogAppService.class.getDeclaredMethod("recordEvent", AuditLogEvent.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
