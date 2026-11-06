// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.repository;

import dev.strata.system.api.entity.DemoProduct;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DemoProductRepository extends JpaRepository<DemoProduct, Long>, JpaSpecificationExecutor<DemoProduct> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Optional<DemoProduct> findBySku(String sku);
}
