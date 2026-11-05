// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One annotation sample ({@code ai_label_sample}) under an {@link AiLabelTask}. {@code content} is the
 * raw payload to label; {@code label} is the annotator's result and {@code source} records where the row
 * came from (upload / crawl / manual).
 */
@Entity
@Table(
        name = "ai_label_sample",
        indexes = {@Index(name = "idx_ai_label_sample_task", columnList = "task_id")})
public class AiLabelSample extends TimestampedEntity {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 256)
    private String label;

    @Column(length = 32)
    private String source;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
