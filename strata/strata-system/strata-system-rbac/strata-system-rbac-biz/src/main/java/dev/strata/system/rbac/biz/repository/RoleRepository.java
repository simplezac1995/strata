// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    Optional<Role> findByCodeAndDeletedFalse(String code);

    List<Role> findByDeletedFalseOrderByIdAsc();

    /** Active roles only 鈥?used when issuing tokens so revoked roles stop granting authorities. */
    List<Role> findByDeletedFalseAndEnabledTrueOrderByIdAsc();

    /**
     * Fuzzy role lookup by name or code (#15). {@code lower()} is portable JPQL.
     */
    @Query("select r from Role r where r.deleted = false and ("
            + "lower(r.name) like lower(concat('%', :kw, '%')) "
            + "or lower(r.code) like lower(concat('%', :kw, '%')))")
    List<Role> search(@Param("kw") String keyword, Pageable pageable);
}
