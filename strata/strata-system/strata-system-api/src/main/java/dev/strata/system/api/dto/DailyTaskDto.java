// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record DailyTaskDto(String id, String name, String taskType, int targetCount, long rewardPoints) {}
