// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A player-facing daily task definition.
 *
 * <p>{@code taskType} is {@code login | recharge | bet | invite}.
 */
@Entity
@Table(name = "daily_task")
public class DailyTask extends TimestampedEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "task_type", nullable = false, length = 16)
    private String taskType = "login";

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "reward_points", nullable = false)
    private long rewardPoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
    }

    public long getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(long rewardPoints) {
        this.rewardPoints = rewardPoints;
    }
}
