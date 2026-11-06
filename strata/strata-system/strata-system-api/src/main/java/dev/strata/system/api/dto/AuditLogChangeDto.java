// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record AuditLogChangeDto(
        String id,
        String operationLogId,
        String entityName,
        String entityId,
        String fieldName,
        String oldValue,
        String newValue,
        Instant createdAt) {}
