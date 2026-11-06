// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.community")
public class CommunityFeaturesProperties {

    /** Community job "Run now" simulates a tick — no Quartz/Spring scheduler. */
    private boolean jobSchedulerStub = true;

    /** Background workflow runner auto-completes PENDING runs (no BPM engine). */
    private boolean workflowRunnerStub = true;

    /** In-memory demo keys when Redis is not configured. */
    private boolean cacheLocalDemo = true;

    public boolean isJobSchedulerStub() {
        return jobSchedulerStub;
    }

    public void setJobSchedulerStub(boolean jobSchedulerStub) {
        this.jobSchedulerStub = jobSchedulerStub;
    }

    public boolean isWorkflowRunnerStub() {
        return workflowRunnerStub;
    }

    public void setWorkflowRunnerStub(boolean workflowRunnerStub) {
        this.workflowRunnerStub = workflowRunnerStub;
    }

    public boolean isCacheLocalDemo() {
        return cacheLocalDemo;
    }

    public void setCacheLocalDemo(boolean cacheLocalDemo) {
        this.cacheLocalDemo = cacheLocalDemo;
    }
}
