// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.repository;

import dev.strata.system.communication.api.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserIdAndReadAtIsNull(Long recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientUserId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * All enabled staff user ids, resolved straight from {@code sys_user} by name so this inbox module does
     * not have to depend on the RBAC business layer. Used only by the admin broadcast fan-out.
     */
    @Query(value = "SELECT id FROM sys_user WHERE deleted = 0 AND enabled = 1", nativeQuery = true)
    List<Long> findActiveStaffUserIds();
}
