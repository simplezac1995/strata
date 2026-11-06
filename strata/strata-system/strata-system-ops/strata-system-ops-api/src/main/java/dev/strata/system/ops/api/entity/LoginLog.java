// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "sys_login_log",
        indexes = {
            @Index(name = "idx_ll_username", columnList = "username"),
            @Index(name = "idx_ll_status", columnList = "status"),
            @Index(name = "idx_ll_created_at", columnList = "created_at"),
            @Index(name = "idx_ll_ip", columnList = "ip"),
            @Index(name = "idx_ll_username_created", columnList = "username, created_at")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLog extends AuditingEntity {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAIL = "fail";

    @Column(nullable = false, length = 64)
    private String username;

    @Column(length = 64)
    private String ip;

    @Column(length = 64)
    private String location;

    @Column(length = 128)
    private String browser;

    @Column(nullable = false, length = 16)
    private String status;
}
