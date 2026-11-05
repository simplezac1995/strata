// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.Announcement;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpsAnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderBySortAscIdAsc();

    @Query(
            """
            select a from OpsAnnouncement a
            where a.status = 'published'
              and (a.startAt is null or a.startAt <= :now)
              and (a.endAt is null or a.endAt >= :now)
            order by a.sort asc, a.id asc
            """)
    List<Announcement> findActive(@Param("now") Instant now);
}
