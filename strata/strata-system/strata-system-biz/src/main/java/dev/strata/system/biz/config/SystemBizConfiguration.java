// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.config;

import dev.strata.system.biz.seed.SeedProperties;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import dev.strata.system.rbac.biz.config.LoginLockProperties;
import dev.strata.system.rbac.biz.config.TokenBlacklistProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    CommunityFeaturesProperties.class,
    LoginLockProperties.class,
    SeedProperties.class,
    TokenBlacklistProperties.class
})
public class SystemBizConfiguration {}
