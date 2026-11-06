// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.repository;

import dev.strata.system.communication.api.entity.Announcement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByCreatedAtDescIdDesc();
}
