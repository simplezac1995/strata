// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.OperationLogDto;
import dev.strata.system.ops.api.entity.OperationLog;
import dev.strata.system.ops.biz.mapper.OperationLogMapper;
import dev.strata.system.ops.biz.repository.OperationLogRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("OperationLogAppService 单元测试")
class OperationLogAppServiceTest {

    @Mock
    private OperationLogRepository operationLogRepository;

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogAppService operationLogAppService;

    @Test
    @DisplayName("分页查询操作日志并映射 DTO")
    void page_ReturnsPagedDtos() {
        OperationLog op = new OperationLog();
        when(operationLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(op)));
        when(operationLogMapper.toDto(any(OperationLog.class)))
                .thenReturn(new OperationLogDto("1", "system:user", "update", "admin", "changed email", Instant.now()));

        PageResult<OperationLogDto> result = operationLogAppService.page(0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).moduleName()).isEqualTo("system:user");
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
