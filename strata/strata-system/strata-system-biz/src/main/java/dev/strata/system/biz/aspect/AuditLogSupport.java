// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.aspect;

public final class AuditLogSupport {

    private AuditLogSupport() {}

    public static String eventTypeForModule(String module) {
        if (module == null || module.isBlank()) {
            return "DATA";
        }
        String prefix = module.contains(":") ? module.substring(0, module.indexOf(':')) : module;
        return switch (prefix) {
            case "system" -> "RBAC";
            case "auth" -> "AUTH";
            case "generator", "tools" -> "CONFIG";
            default -> "DATA";
        };
    }

    public static String resourceTypeForModule(String module) {
        if (module == null || !module.contains(":")) {
            return null;
        }
        return module.substring(module.indexOf(':') + 1);
    }
}
