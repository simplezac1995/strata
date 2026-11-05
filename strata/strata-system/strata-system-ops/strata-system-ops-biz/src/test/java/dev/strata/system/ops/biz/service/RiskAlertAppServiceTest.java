// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.RiskAlertDto;
import dev.strata.system.ops.api.entity.RiskAlert;
import dev.strata.system.ops.biz.mapper.RiskAlertMapper;
import dev.strata.system.ops.biz.repository.RiskAlertRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAlertAppService 单元测试")
@SuppressWarnings({"unchecked", "varargs"})
class RiskAlertAppServiceTest {

    @Mock
    private RiskAlertRepository riskAlertRepository;

    @Mock
    private RiskAlertMapper riskAlertMapper;

    @InjectMocks
    private RiskAlertAppService riskAlertAppService;

    @Test
    @DisplayName("按状态分页查询告警")
    void page_FiltersByStatus() {
        RiskAlert alert = new RiskAlert();
        when(riskAlertRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alert)));
        when(riskAlertMapper.toDto(any(RiskAlert.class)))
                .thenReturn(new RiskAlertDto("1", null, "127.0.0.1", "h", "r", "d", "open", null));

        PageResult<RiskAlertDto> result = riskAlertAppService.page("open", 0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("解决 open 告警置为 resolved 并保存")
    void resolve_Open_Resolves() {
        RiskAlert alert = new RiskAlert();
        alert.setStatus("open");
        when(riskAlertRepository.findById(5L)).thenReturn(Optional.of(alert));
        when(riskAlertMapper.toDto(alert))
                .thenReturn(new RiskAlertDto("5", null, null, null, null, null, "resolved", null));

        RiskAlertDto dto = riskAlertAppService.resolve(5L);

        assertThat(dto.status()).isEqualTo("resolved");
        verify(riskAlertRepository).save(alert);
    }

    @Test
    @DisplayName("解决不存在告警时抛 NOT_FOUND")
    void resolve_Missing_ThrowsNotFound() {
        when(riskAlertRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskAlertAppService.resolve(9L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("解决已 resolved 告警时抛状态非法")
    void resolve_AlreadyResolved_ThrowsInvalidState() {
        RiskAlert alert = new RiskAlert();
        alert.setStatus("resolved");
        when(riskAlertRepository.findById(5L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> riskAlertAppService.resolve(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already");
    }
}
