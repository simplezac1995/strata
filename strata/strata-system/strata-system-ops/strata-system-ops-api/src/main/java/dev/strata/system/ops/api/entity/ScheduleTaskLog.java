// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One execution of a scheduled task ({@code schedule_task_log}). Captures when the run started/ended, how long it
 * took, and whether it succeeded — so the admin UI can show a real execution history instead of a stubbed tick.
 */
@Entity
@Table(
        name = "schedule_task_log",
        indexes = {
            @Index(name = "idx_stl_job_id", columnList = "job_id"),
            @Index(name = "idx_stl_status", columnList = "status")
        })
public class ScheduleTaskLog extends TimestampedEntity {

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "job_name", nullable = false, length = 128)
    private String jobName;

    /** SUCCESS | FAILURE. */
    @Column(nullable = false, length = 16)
    private String status = "SUCCESS";

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(length = 1024)
    private String message;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
