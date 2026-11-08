// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.LoginLogDto;
import dev.strata.system.api.dto.LoginLogQuery;
import dev.strata.system.ops.api.entity.LoginLog;
import dev.strata.system.ops.biz.mapper.LoginLogMapper;
import dev.strata.system.ops.biz.repository.LoginLogRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class LoginLogAppServiceTest {

    @Mock
    private LoginLogRepository loginLogRepository;

    @Mock
    private LoginLogMapper loginLogMapper;

    @InjectMocks
    private LoginLogAppService loginLogAppService;

    @Test
    void page_WithFilters_ReturnsPagedDtos() {
        LoginLog row = new LoginLog();
        LoginLogQuery query = new LoginLogQuery(
                "admin", "SUCCESS", "10.0.0.1", Instant.now().minusSeconds(3600), Instant.now(), 0, 10);
        when(loginLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(loginLogMapper.toDto(any(LoginLog.class)))
                .thenReturn(new LoginLogDto("1", "admin", "10.0.0.1", "Chrome 120", "SUCCESS", Instant.now()));

        PageResult<LoginLogDto> result = loginLogAppService.page(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).username()).isEqualTo("admin");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void page_NullFilters_Empty() {
        LoginLogQuery query = new LoginLogQuery(null, null, null, null, null, 0, 10);
        when(loginLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResult<LoginLogDto> result = loginLogAppService.page(query);

        assertThat(result.content()).isEmpty();
    }
}
