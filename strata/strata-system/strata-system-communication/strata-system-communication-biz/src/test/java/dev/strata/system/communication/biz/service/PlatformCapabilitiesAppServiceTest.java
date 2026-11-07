// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.capabilities.PlatformCapabilitiesContributor;
import dev.strata.system.api.dto.PlatformCapabilitiesDto;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformCapabilitiesAppServiceTest {

    @Test
    void capabilities_NoContributors_ReturnsBase() {
        CommunityFeaturesProperties props = new CommunityFeaturesProperties();
        PlatformCapabilitiesAppService service = new PlatformCapabilitiesAppService(props, List.of());

        PlatformCapabilitiesDto dto = service.capabilities();

        assertThat(dto.jobSchedulerStub()).isTrue();
        assertThat(dto.workflowRunnerStub()).isTrue();
        assertThat(dto.cacheLocalDemo()).isTrue();
        assertThat(dto.edition()).isEqualTo("community");
    }

    @Test
    void capabilities_WithContributors_AppliesInOrder() {
        CommunityFeaturesProperties props = new CommunityFeaturesProperties();
        PlatformCapabilitiesContributor editionOverride = base -> new PlatformCapabilitiesDto(
                base.jobSchedulerStub(), base.workflowRunnerStub(), base.cacheLocalDemo(), true, true, "enterprise");
        PlatformCapabilitiesContributor flagFlip = base -> new PlatformCapabilitiesDto(
                false,
                base.workflowRunnerStub(),
                base.cacheLocalDemo(),
                base.rowLevelTenantEnabled(),
                base.workflowEngineAvailable(),
                base.edition());
        PlatformCapabilitiesAppService service =
                new PlatformCapabilitiesAppService(props, List.of(editionOverride, flagFlip));

        PlatformCapabilitiesDto dto = service.capabilities();

        assertThat(dto.edition()).isEqualTo("enterprise");
        assertThat(dto.rowLevelTenantEnabled()).isTrue();
        assertThat(dto.workflowEngineAvailable()).isTrue();
        assertThat(dto.jobSchedulerStub()).isFalse();
    }
}
