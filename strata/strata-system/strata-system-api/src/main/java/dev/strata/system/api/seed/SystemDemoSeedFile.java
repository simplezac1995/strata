// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.seed;

import java.util.List;

public record SystemDemoSeedFile(
        List<RoleSeed> roles,
        List<UserSeed> users,
        List<UserRoleSeed> userRoles,
        List<RoleMenuSeed> roleMenus,
        List<DepartmentSeed> departments,
        List<ProductSeed> products,
        List<PaymentSeed> payments,
        List<LoginLogSeed> loginLogs,
        List<OperationLogSeed> operationLogs,
        List<AuditLogSeed> auditLogs,
        List<ApiKeySeed> apiKeys,
        List<NotificationPreferenceSeed> notificationPreferences) {

    public record RoleSeed(String code, String name, boolean enabled) {}

    public record UserSeed(
            String username, String password, String nickname, String email, String tenantCode, boolean enabled) {}

    public record UserRoleSeed(String username, String roleCode) {}

    public record RoleMenuSeed(String roleCode, String permission) {}

    public record DepartmentSeed(String name, String parentName, int sortOrder, boolean enabled) {}

    public record ProductSeed(String name, String sku, String price, int stock, String description, boolean enabled) {}

    public record PaymentSeed(String orderNo, long amountCents, String currency, String status, String sessionId) {}

    public record LoginLogSeed(String username, String ip, String location, String browser, String status) {}

    public record OperationLogSeed(String username, String moduleName, String actionName, String detail) {}

    public record AuditLogSeed(
            String username,
            String eventType,
            String moduleName,
            String actionName,
            String resourceType,
            String resourceId,
            String ipAddress,
            String userAgent,
            boolean success,
            String detail) {}

    public record ApiKeySeed(String name, String plaintext, String scopes, boolean enabled) {}

    public record NotificationPreferenceSeed(
            String username,
            boolean emailEnabled,
            boolean inAppEnabled,
            boolean marketingEnabled,
            boolean securityEnabled) {}
}
