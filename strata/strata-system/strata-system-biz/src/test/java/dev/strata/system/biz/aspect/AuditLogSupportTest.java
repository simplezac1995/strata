// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuditLogSupportTest {

    @Test
    void eventTypeForSystemModuleIsRbac() {
        assertEquals("RBAC", AuditLogSupport.eventTypeForModule("system:user"));
    }

    @Test
    void eventTypeForDemoModuleIsData() {
        assertEquals("DATA", AuditLogSupport.eventTypeForModule("demo:product"));
    }

    @Test
    void resourceTypeExtractsSuffix() {
        assertEquals("user", AuditLogSupport.resourceTypeForModule("system:user"));
    }
}
