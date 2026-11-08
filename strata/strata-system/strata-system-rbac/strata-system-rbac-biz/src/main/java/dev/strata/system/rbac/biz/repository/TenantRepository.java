// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.Tenant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    List<Tenant> findAllByOrderByIdAsc();

    boolean existsByCode(String code);
}
