// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/**
 * Real-time system health snapshot, probed from DataSource / RedisTemplate / JDK MXBeans.
 */
public record OpsHealthDto(
        ComponentHealth db, ComponentHealth redis, DiskHealth disk, CpuHealth cpu, MemoryHealth memory, long uptimeMs) {

    public record ComponentHealth(String status, long responseMs) {}

    public record DiskHealth(double totalGb, double usedGb, double freeGb) {}

    public record CpuHealth(double usagePercent) {}

    public record MemoryHealth(long usedMb, long totalMb, double usagePercent) {}
}
