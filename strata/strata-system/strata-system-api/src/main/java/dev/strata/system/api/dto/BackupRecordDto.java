// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record BackupRecordDto(
        String id, String filename, long sizeBytes, String type, String status, String path, Instant createdAt) {}
