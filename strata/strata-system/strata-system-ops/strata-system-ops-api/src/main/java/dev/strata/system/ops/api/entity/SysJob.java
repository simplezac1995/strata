// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "sys_job",
        indexes = {
            @Index(name = "idx_job_status", columnList = "status"),
            @Index(name = "idx_job_group", columnList = "job_group")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class SysJob extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "job_group", nullable = false, length = 64)
    private String jobGroup = "DEFAULT";

    @Column(nullable = false, length = 64)
    private String cron;

    /** RUNNING | PAUSED | ERROR */
    @Column(nullable = false, length = 16)
    private String status = "PAUSED";

    @Column(name = "last_run")
    private Instant lastRun;

    @Column(name = "next_run")
    private Instant nextRun;

    @Column(nullable = false)
    private boolean enabled = true;
}
