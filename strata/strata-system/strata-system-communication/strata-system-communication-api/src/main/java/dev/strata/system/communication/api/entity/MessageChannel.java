// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_message_channel")
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageChannel extends AuditingEntity {

    /** email | sms | webhook | internal */
    @Column(name = "channel_type", nullable = false, unique = true, length = 32)
    private String channelType;

    @Column(name = "config_json", length = 4000)
    private String configJson;

    @Column(nullable = false)
    private boolean enabled = false;
}
