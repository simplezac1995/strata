// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record AuditLogDto(
        String id,
        String eventType,
        String moduleName,
        String actionName,
        String resourceType,
        String resourceId,
        String operatorName,
        String ipAddress,
        boolean success,
        String detail,
        String changedFields,
        Instant createdAt) {}
