// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.dto.AuditLogDto;
import dev.strata.system.api.dto.JobDto;
import dev.strata.system.api.dto.LoginLogDto;
import dev.strata.system.api.dto.OperationLogDto;
import dev.strata.system.api.event.LoginLogEvent;
import dev.strata.system.api.request.JobCreateRequest;
import dev.strata.system.api.request.JobUpdateRequest;
import dev.strata.system.ops.api.entity.AuditLog;
import dev.strata.system.ops.api.entity.LoginLog;
import dev.strata.system.ops.api.entity.OperationLog;
import dev.strata.system.ops.api.entity.SysJob;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Ops 模块 Mapper 接口契约单元测试：覆盖 null-check 与 defaultValue 分支，不启动 Spring 容器。
 */
class OpsMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final JobMapper jobMapper = new JobMapperImpl();
    private final AuditLogMapper auditLogMapper = new AuditLogMapperImpl();
    private final OperationLogMapper operationLogMapper = new OperationLogMapperImpl();
    private final LoginLogMapper loginLogMapper = new LoginLogMapperImpl();

    @Nested
    @DisplayName("JobMapper")
    class JobMapperCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(jobMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity jobGroup 与 enabled 显式非空时使用请求值，status 固定 PAUSED")
        void toEntityExplicit() {
            SysJob job = jobMapper.toEntity(new JobCreateRequest("cleanup", "batch", "0 0 * * *", false));
            assertThat(job.getName()).isEqualTo("cleanup");
            assertThat(job.getJobGroup()).isEqualTo("batch");
            assertThat(job.getCron()).isEqualTo("0 0 * * *");
            assertThat(job.getStatus()).isEqualTo("PAUSED");
            assertThat(job.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("toEntity jobGroup/enabled 为 null 时走 defaultValue 分支")
        void toEntityDefaults() {
            SysJob job = jobMapper.toEntity(new JobCreateRequest("cleanup", null, "0 0 * * *", null));
            assertThat(job.getJobGroup()).isEqualTo("DEFAULT");
            assertThat(job.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支")
        void toDtoBranches() {
            assertThat(jobMapper.toDto(null)).isNull();

            SysJob job = new SysJob();
            job.setId(3L);
            job.setName("cleanup");
            job.setJobGroup("batch");
            job.setCron("0 0 * * *");
            job.setStatus("RUNNING");
            job.setLastRun(NOW);
            job.setNextRun(NOW.plusSeconds(60));
            job.setEnabled(true);
            JobDto dto = jobMapper.toDto(job);
            assertThat(dto.id()).isEqualTo("3");
            assertThat(dto.name()).isEqualTo("cleanup");
            assertThat(dto.jobGroup()).isEqualTo("batch");
            assertThat(dto.cron()).isEqualTo("0 0 * * *");
            assertThat(dto.status()).isEqualTo("RUNNING");
            assertThat(dto.lastRun()).isEqualTo(NOW);
            assertThat(dto.nextRun()).isEqualTo(NOW.plusSeconds(60));
            assertThat(dto.enabled()).isTrue();

            job.setId(null);
            assertThat(jobMapper.toDto(job).id()).isNull();
        }

        @Test
        @DisplayName("update(null, 任务) 不改；可空字段 null 时保持原值")
        void updateBranches() {
            SysJob job = new SysJob();
            job.setName("old");
            job.setJobGroup("oldgroup");
            job.setCron("* * * * *");
            job.setEnabled(true);

            jobMapper.update(null, job);
            assertThat(job.getName()).isEqualTo("old");

            jobMapper.update(new JobUpdateRequest("new", "newgroup", "0 0 0 * *", false), job);
            assertThat(job.getName()).isEqualTo("new");
            assertThat(job.getJobGroup()).isEqualTo("newgroup");
            assertThat(job.getCron()).isEqualTo("0 0 0 * *");
            assertThat(job.isEnabled()).isFalse();

            jobMapper.update(new JobUpdateRequest(null, null, null, null), job);
            assertThat(job.getName()).isEqualTo("new");
            assertThat(job.getJobGroup()).isEqualTo("newgroup");
            assertThat(job.getCron()).isEqualTo("0 0 0 * *");
            assertThat(job.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("AuditLogMapper")
    class AuditLogMapperCases {

        @Test
        @DisplayName("toDto(null) 返回 null")
        void toDtoNull() {
            assertThat(auditLogMapper.toDto(null)).isNull();
        }

        @Test
        @DisplayName("toDto 全字段映射")
        void toDtoFull() {
            AuditLog record = new AuditLog();
            record.setId(100L);
            record.setEventType("AUTH");
            record.setModuleName("rbac");
            record.setActionName("LOGIN");
            record.setResourceType("user");
            record.setResourceId("1");
            record.setOperatorName("alice");
            record.setIpAddress("127.0.0.1");
            record.setSuccess(true);
            record.setDetail("ok");
            record.setChangedFields("{}");
            record.setCreatedAt(NOW);
            AuditLogDto dto = auditLogMapper.toDto(record);
            assertThat(dto.id()).isEqualTo("100");
            assertThat(dto.eventType()).isEqualTo("AUTH");
            assertThat(dto.moduleName()).isEqualTo("rbac");
            assertThat(dto.actionName()).isEqualTo("LOGIN");
            assertThat(dto.resourceType()).isEqualTo("user");
            assertThat(dto.resourceId()).isEqualTo("1");
            assertThat(dto.operatorName()).isEqualTo("alice");
            assertThat(dto.ipAddress()).isEqualTo("127.0.0.1");
            assertThat(dto.success()).isTrue();
            assertThat(dto.detail()).isEqualTo("ok");
            assertThat(dto.changedFields()).isEqualTo("{}");
            assertThat(dto.createdAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("OperationLogMapper")
    class OperationLogMapperCases {

        @Test
        @DisplayName("toDto(null) 返回 null")
        void toDtoNull() {
            assertThat(operationLogMapper.toDto(null)).isNull();
        }

        @Test
        @DisplayName("toDto 全字段映射")
        void toDtoFull() {
            OperationLog record = new OperationLog();
            record.setId(200L);
            record.setModuleName("rbac");
            record.setActionName("UPDATE");
            record.setOperatorName("bob");
            record.setDetail("changed");
            record.setCreatedAt(NOW);
            OperationLogDto dto = operationLogMapper.toDto(record);
            assertThat(dto.id()).isEqualTo("200");
            assertThat(dto.moduleName()).isEqualTo("rbac");
            assertThat(dto.actionName()).isEqualTo("UPDATE");
            assertThat(dto.operatorName()).isEqualTo("bob");
            assertThat(dto.detail()).isEqualTo("changed");
            assertThat(dto.createdAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("LoginLogMapper")
    class LoginLogMapperCases {

        @Test
        @DisplayName("toEntity(null) 返回 null")
        void toEntityNull() {
            assertThat(loginLogMapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity 全字段映射")
        void toEntityFull() {
            LoginLog log = loginLogMapper.toEntity(new LoginLogEvent("alice", "127.0.0.1", "Chrome", "success"));
            assertThat(log.getUsername()).isEqualTo("alice");
            assertThat(log.getIp()).isEqualTo("127.0.0.1");
            assertThat(log.getBrowser()).isEqualTo("Chrome");
            assertThat(log.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("toDto(null) 返回 null；全字段与 null id 分支")
        void toDtoBranches() {
            assertThat(loginLogMapper.toDto(null)).isNull();

            LoginLog log = new LoginLog();
            log.setId(50L);
            log.setUsername("alice");
            log.setIp("127.0.0.1");
            log.setBrowser("Chrome");
            log.setStatus("success");
            log.setCreatedAt(NOW);
            LoginLogDto dto = loginLogMapper.toDto(log);
            assertThat(dto.id()).isEqualTo("50");
            assertThat(dto.username()).isEqualTo("alice");
            assertThat(dto.ip()).isEqualTo("127.0.0.1");
            assertThat(dto.browser()).isEqualTo("Chrome");
            assertThat(dto.status()).isEqualTo("success");
            assertThat(dto.createdAt()).isEqualTo(NOW);

            log.setId(null);
            assertThat(loginLogMapper.toDto(log).id()).isNull();
        }
    }
}
