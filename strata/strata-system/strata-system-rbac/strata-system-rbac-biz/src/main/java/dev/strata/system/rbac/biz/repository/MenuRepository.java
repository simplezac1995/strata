// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.Menu;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<Menu> findAllByOrderBySortOrderAscIdAsc();

    /**
     * Enabled menus whose id is in {@code ids}, already sorted. Replaces loading every enabled menu and filtering
     * in memory (P1-12): at login only the menus the user's roles grant are wanted.
     */
    List<Menu> findByIdInAndEnabledTrueOrderBySortOrderAscIdAsc(Collection<Long> ids);

    boolean existsByPermission(String permission);

    Optional<Menu> findFirstByPathAndType(String path, String type);

    List<Menu> findByParentIdIsNullOrderBySortOrderAscIdAsc();

    List<Menu> findByParentIdOrderBySortOrderAscIdAsc(Long parentId);

    boolean existsByParentIdAndPath(Long parentId, String path);

    @Query(
            """
            SELECT m FROM Menu m
            WHERE (:keyword IS NULL OR :keyword = ''
                OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(m.path, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(m.permission, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Menu> search(@Param("keyword") String keyword, Pageable pageable);
}
