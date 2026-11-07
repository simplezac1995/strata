// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link ScimProperties}; the SCIM endpoints stay disabled until configured. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ScimProperties.class)
public class ScimConfiguration {}
