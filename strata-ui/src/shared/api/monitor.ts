// SPDX-License-Identifier: Apache-2.0
import { apiGet } from "@/shared/api/client";

/* ─── JVM runtime monitor snapshot (contract-aligned, do not guess fields) ── */

export interface MemoryUsageDto {
  used: number;
  committed: number;
  max: number;
}

export interface GcDto {
  name: string;
  collectionCount: number;
  collectionTimeMs: number;
}

export interface ThreadsDto {
  current: number;
  peak: number;
  daemon: number;
  totalStarted: number;
}

export interface PoolDto {
  poolName: string;
  active: number;
  idle: number;
  pending: number;
  max: number;
  minIdle: number;
}

export interface MonitorSnapshotDto {
  heap: MemoryUsageDto;
  nonHeap: MemoryUsageDto;
  gc: GcDto[];
  threads: ThreadsDto;
  pool: PoolDto | null;
  recentResponses: unknown[];
}

export const monitorApi = {
  snapshot: () => apiGet<MonitorSnapshotDto>("/api/v1/system/monitor/snapshot"),
};
