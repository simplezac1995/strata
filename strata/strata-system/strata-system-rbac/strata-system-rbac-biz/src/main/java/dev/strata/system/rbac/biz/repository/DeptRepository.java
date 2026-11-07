// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.Dept;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeptRepository extends JpaRepository<Dept, Long> {

    List<Dept> findAllByOrderBySortOrderAscIdAsc();

    long countByParentId(Long parentId);

    List<Dept> findByParentIdIsNullOrderBySortOrderAscIdAsc();

    List<Dept> findByParentIdOrderBySortOrderAscIdAsc(Long parentId);

    @Query(
            """
            SELECT d FROM Dept d
            WHERE (:keyword IS NULL OR :keyword = ''
                OR LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Dept> search(@Param("keyword") String keyword, Pageable pageable);
}
