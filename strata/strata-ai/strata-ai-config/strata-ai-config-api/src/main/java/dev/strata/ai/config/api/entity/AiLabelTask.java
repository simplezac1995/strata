// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A data-labeling task ({@code ai_label_task}). {@code type} is an open short string
 * ({@code text}/{@code image}/{@code audio}) and {@code status} one of
 * {@code pending}/{@code in_progress}/{@code completed}. Rows are soft-deleted via {@link #deleted};
 * the annotation samples live in {@link AiLabelSample}.
 */
@Entity
@Table(
        name = "ai_label_task",
        indexes = {
            @Index(name = "idx_ai_label_task_deleted", columnList = "deleted"),
            @Index(name = "idx_ai_label_task_status", columnList = "status")
        })
public class AiLabelTask extends TimestampedEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String type = "text";

    @Column(nullable = false, length = 16)
    private String status = "pending";

    @Column(name = "sample_count", nullable = false)
    private int sampleCount = 0;

    @Column(nullable = false)
    private boolean deleted = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
