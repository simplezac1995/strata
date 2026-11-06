// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link SocialLoginProperties}; the social/OIDC endpoints stay disabled until configured. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SocialLoginProperties.class)
public class SocialLoginConfiguration {}
