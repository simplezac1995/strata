// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import dev.strata.system.api.capabilities.PlatformCapabilitiesContributor;
import dev.strata.system.api.dto.PlatformCapabilitiesDto;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlatformCapabilitiesAppService {

    private static final Logger log = LoggerFactory.getLogger(PlatformCapabilitiesAppService.class);

    private final CommunityFeaturesProperties communityFeatures;
    private final List<PlatformCapabilitiesContributor> contributors;

    public PlatformCapabilitiesAppService(
            CommunityFeaturesProperties communityFeatures, List<PlatformCapabilitiesContributor> contributors) {
        this.communityFeatures = communityFeatures;
        this.contributors = contributors;
    }

    public PlatformCapabilitiesDto capabilities() {
        PlatformCapabilitiesDto base = new PlatformCapabilitiesDto(
                communityFeatures.isJobSchedulerStub(),
                communityFeatures.isWorkflowRunnerStub(),
                communityFeatures.isCacheLocalDemo());
        PlatformCapabilitiesDto dto = base;
        for (PlatformCapabilitiesContributor contributor : contributors) {
            dto = contributor.apply(dto);
        }
        return dto;
    }
}
