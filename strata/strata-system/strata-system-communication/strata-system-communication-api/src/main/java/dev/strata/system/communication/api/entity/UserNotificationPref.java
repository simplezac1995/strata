// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "sys_user_notification_pref")
@Data
public class UserNotificationPref {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled = false;

    @Column(name = "security_enabled", nullable = false)
    private boolean securityEnabled = true;
}
