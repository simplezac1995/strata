// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.User;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsernameAndDeletedFalse(String username);

    List<User> findByDeletedFalseOrderByIdAsc();

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    long countByDeletedFalse();

    /**
     * Projection of registration timestamps for users created on/after {@code from} (#6 trend). Selecting
     * only the timestamp column avoids hydrating credential/totp columns. Bucket by day in the service layer
     * to keep date bucketing in Java rather than relying on database-specific date functions.
     */
    @Query("select u.createdAt from User u where u.deleted = false and u.createdAt >= :from")
    List<Instant> findCreatedAtAfter(@Param("from") Instant from);

    /**
     * Fuzzy staff-user lookup by username or nickname (#15). {@code lower()} is portable JPQL.
     */
    @Query("select u from User u where u.deleted = false and ("
            + "lower(u.username) like lower(concat('%', :kw, '%')) "
            + "or lower(coalesce(u.nickname, '')) like lower(concat('%', :kw, '%')))")
    List<User> search(@Param("kw") String keyword, Pageable pageable);

    /**
     * One bulk soft-delete for a set of ids (P2-21): flips {@code deleted} and {@code enabled} in a single
     * {@code UPDATE} instead of loading each row and issuing N statements. Already-deleted rows are left
     * untouched. Returns the number of rows actually updated.
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = true, u.enabled = false WHERE u.id IN :ids AND u.deleted = false")
    int softDeleteByIds(@Param("ids") Collection<Long> ids);
}
