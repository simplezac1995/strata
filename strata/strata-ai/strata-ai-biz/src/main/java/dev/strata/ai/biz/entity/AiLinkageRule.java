// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * An AI linkage rule ({@code ai_linkage_rule}): when a business object emits a configured event, the AI
 * takes a configured action (notify / tag / email). Pure admin configuration rows; the rule engine itself
 * only evaluates enabled rows.
 */
@Entity
@Table(
        name = "ai_linkage_rule",
        indexes = {
            @Index(name = "idx_ai_linkage_obj_event", columnList = "object, event"),
            @Index(name = "idx_ai_linkage_enabled", columnList = "enabled")
        })
public class AiLinkageRule extends AuditingEntity {

    @Column(nullable = false, length = 128)
    private String name;

    /** user | order | recharge | withdraw. */
    @Column(nullable = false, length = 32)
    private String object;

    @Column(nullable = false, length = 64)
    private String event;

    /** notify | tag | email. */
    @Column(nullable = false, length = 16)
    private String action;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 512)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
