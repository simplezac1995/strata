// SPDX-License-Identifier: Apache-2.0
package dev.strata.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * What a consumer gets from adding {@code strata-spring-boot-starter}: the framework's system modules are scanned,
 * their entities and Spring Data repositories are registered, and scheduling is on.
 *
 * <p>Why this class exists: the platform modules that ship their own
 * {@code META-INF/spring/...AutoConfiguration.imports} (the common kernel, security, Redisson and the opt-in plugins)
 * are discovered automatically. The system domain does not ship one - the product application scanned it by hand -
 * so without this class a consumer would get the kernel but silently miss RBAC, settings, notifications and ops.
 *
 * <p>The product application keeps its own {@code scanBasePackages = "dev.strata"} and therefore sets
 * {@code strata.starter.auto-configuration=false}: scanning the same packages twice would register every bean
 * twice and merge entity-scan packages incorrectly. Consumers that only depend on the starter leave it at the
 * default ({@code true}).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnProperty(
        prefix = "strata.starter",
        name = "auto-configuration",
        havingValue = "true",
        matchIfMissing = true)
@ComponentScan(
        basePackages = {
            "dev.strata.system",
            "dev.strata.common.security.audit",
            "dev.strata.common.security.crypto",
            "dev.strata.common.security.ratelimit",
            "dev.strata.common.security.signature"
        })
@EntityScan(basePackages = "dev.strata.system")
@EnableJpaRepositories(basePackages = "dev.strata.system")
@EnableScheduling
public class StrataAutoConfiguration {}
