// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One sampled {@code /api/v1/**} request, written asynchronously by {@code ApiStatsFilter}.
 *
 * <p>{@code path} is the normalized request URI (query string stripped); {@code statusCode} is the HTTP status
 * returned; {@code durationMs} is wall-clock time inside the filter chain. The row is append-only: the filter
 * inserts it after the response commits, it is never updated.
 */
@Entity
@Table(
        name = "api_call_stat",
        indexes = {
            @Index(name = "idx_api_call_stat_path", columnList = "path"),
            @Index(name = "idx_api_call_stat_created", columnList = "created_at")
        })
public class ApiCallStat extends TimestampedEntity {

    @Column(nullable = false, length = 256)
    private String path;

    @Column(nullable = false, length = 8)
    private String method;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(length = 64)
    private String ip;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
