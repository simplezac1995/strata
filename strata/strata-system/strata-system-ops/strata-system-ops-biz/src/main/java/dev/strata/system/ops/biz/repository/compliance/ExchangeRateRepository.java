// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository.compliance;

import dev.strata.system.ops.api.entity.compliance.ExchangeRate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExchangeRateRepository
        extends JpaRepository<ExchangeRate, Long>, JpaSpecificationExecutor<ExchangeRate> {

    List<ExchangeRate> findAllByOrderByFromCurrencyAscToCurrencyAsc();
}
