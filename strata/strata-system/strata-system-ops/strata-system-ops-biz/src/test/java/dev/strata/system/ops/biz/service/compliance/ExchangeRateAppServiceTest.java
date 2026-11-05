// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.request.compliance.ExchangeRateCreateRequest;
import dev.strata.system.api.request.compliance.ExchangeRateUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.ExchangeRate;
import dev.strata.system.ops.biz.mapper.compliance.ExchangeRateMapper;
import dev.strata.system.ops.biz.repository.compliance.ExchangeRateRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateAppService 单元测试")
class ExchangeRateAppServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private ExchangeRateAppService service;

    @Test
    @DisplayName("列表返回全部汇率")
    void list_ReturnsAll() {
        when(exchangeRateRepository.findAllByOrderByFromCurrencyAscToCurrencyAsc())
                .thenReturn(List.of(new ExchangeRate()));
        when(exchangeRateMapper.toDto(any(ExchangeRate.class)))
                .thenReturn(new dev.strata.system.api.dto.compliance.ExchangeRateDto(
                        "1", "USDT", "CNY", new BigDecimal("7.2"), null, null));
        assertThat(service.list()).hasSize(1);
    }

    @Test
    @DisplayName("创建汇率落库")
    void create_Persists() {
        ExchangeRateCreateRequest req = new ExchangeRateCreateRequest("USDT", "CNY", new BigDecimal("7.2"));
        ExchangeRate row = new ExchangeRate();
        when(exchangeRateMapper.toEntity(req)).thenReturn(row);
        when(exchangeRateRepository.save(row)).thenReturn(row);
        service.create(req);
        verify(exchangeRateRepository).save(row);
    }

    @Test
    @DisplayName("更新汇率存在时改 rate")
    void update_Existing_SetsRate() {
        ExchangeRate row = new ExchangeRate();
        when(exchangeRateRepository.findById(5L)).thenReturn(Optional.of(row));
        when(exchangeRateRepository.save(row)).thenReturn(row);
        service.update(5L, new ExchangeRateUpdateRequest(new BigDecimal("7.3")));
        assertThat(row.getRate()).isEqualByComparingTo("7.3");
    }

    @Test
    @DisplayName("更新不存在抛 NOT_FOUND")
    void update_Missing_Throws() {
        when(exchangeRateRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(9L, new ExchangeRateUpdateRequest(new BigDecimal("7.3"))))
                .isInstanceOf(BusinessException.class);
    }
}
