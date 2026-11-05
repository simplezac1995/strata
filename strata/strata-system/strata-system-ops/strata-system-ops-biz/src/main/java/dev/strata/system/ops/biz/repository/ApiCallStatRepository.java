// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.ApiCallStat;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Reads the {@code api_call_stat} append-only log. Aggregations are expressed in JPQL so they run unchanged on
 * PostgreSQL; error counts use a portable {@code CASE WHEN} projection.
 */
public interface ApiCallStatRepository extends JpaRepository<ApiCallStat, Long> {

    /**
     * Totals for the trailing window: {@code [totalCalls, avgDurationMs, errorCount]}. {@code avgDurationMs} is
     * {@code null} when the window is empty.
     */
    @Query("select count(t), avg(t.durationMs), "
            + "sum(case when t.statusCode >= 400 then 1 else 0 end) "
            + "from ApiCallStat t where t.createdAt >= :since")
    Object[] totalsSince(@Param("since") Instant since);

    /**
     * Per-path rows for the trailing window: {@code [path, calls, avgDurationMs, errorCount]} ordered by call
     * volume descending.
     */
    @Query("select t.path, count(t), avg(t.durationMs), "
            + "sum(case when t.statusCode >= 400 then 1 else 0 end) "
            + "from ApiCallStat t where t.createdAt >= :since "
            + "group by t.path order by count(t) desc")
    List<Object[]> perPathSince(@Param("since") Instant since);
}
