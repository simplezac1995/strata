// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

public record PlatformCapabilitiesDto(
        boolean jobSchedulerStub,
        boolean workflowRunnerStub,
        boolean cacheLocalDemo,
        boolean rowLevelTenantEnabled,
        boolean workflowEngineAvailable,
        String edition) {

    public PlatformCapabilitiesDto(boolean jobSchedulerStub, boolean workflowRunnerStub, boolean cacheLocalDemo) {
        this(jobSchedulerStub, workflowRunnerStub, cacheLocalDemo, false, false, "community");
    }
}
