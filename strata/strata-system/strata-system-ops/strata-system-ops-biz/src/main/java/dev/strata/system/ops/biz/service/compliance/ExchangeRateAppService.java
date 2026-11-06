// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.system.api.dto.compliance.ExchangeRateDto;
import dev.strata.system.api.request.compliance.ExchangeRateCreateRequest;
import dev.strata.system.api.request.compliance.ExchangeRateUpdateRequest;
import dev.strata.system.ops.api.entity.compliance.ExchangeRate;
import dev.strata.system.ops.biz.mapper.compliance.ExchangeRateMapper;
import dev.strata.system.ops.biz.repository.compliance.ExchangeRateRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Currency exchange rate management. */
@Service
public class ExchangeRateAppService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateAppService.class);

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;

    public ExchangeRateAppService(
            ExchangeRateRepository exchangeRateRepository, ExchangeRateMapper exchangeRateMapper) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.exchangeRateMapper = exchangeRateMapper;
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDto> list() {
        return exchangeRateRepository.findAllByOrderByFromCurrencyAscToCurrencyAsc().stream()
                .map(exchangeRateMapper::toDto)
                .toList();
    }

    @Transactional
    @OperationLog(module = "compliance:rate", action = "create")
    public ExchangeRateDto create(ExchangeRateCreateRequest request) {
        ExchangeRate row = exchangeRateMapper.toEntity(request);
        exchangeRateRepository.save(row);
        return exchangeRateMapper.toDto(row);
    }

    @Transactional
    @OperationLog(module = "compliance:rate", action = "update")
    public ExchangeRateDto update(Long id, ExchangeRateUpdateRequest request) {
        ExchangeRate row =
                exchangeRateRepository.findById(id).orElseThrow(() -> new NotFoundException("Exchange rate not found"));
        row.setRate(request.rate());
        exchangeRateRepository.save(row);
        return exchangeRateMapper.toDto(row);
    }
}
