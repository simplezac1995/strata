// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Metadata record for a database backup artifact.
 *
 * <p>{@code type} is {@code full | incremental}; {@code status} is
 * {@code completed | failed | in_progress}. The actual pg_dump execution is a TODO.
 */
@Entity
@Table(
        name = "backup_record",
        indexes = {@Index(name = "idx_backup_status", columnList = "status")})
public class BackupRecord extends TimestampedEntity {

    @Column(nullable = false, length = 256)
    private String filename;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 16)
    private String type = "full";

    @Column(nullable = false, length = 16)
    private String status = "in_progress";

    @Column(length = 512)
    private String path;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
