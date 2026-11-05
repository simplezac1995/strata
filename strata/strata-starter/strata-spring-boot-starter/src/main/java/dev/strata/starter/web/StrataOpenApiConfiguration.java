// SPDX-License-Identifier: Apache-2.0
package dev.strata.starter.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * OpenAPI/Swagger for framework consumers: one {@link OpenAPI} document with bearer authentication and grouped views
 * for the framework HTTP surfaces. Swagger UI is available at {@code /swagger-ui.html}.
 *
 * <p>Gated by the same starter switch as the rest of the auto-configuration; a host that scans the framework itself
 * (the distribution application) keeps its own OpenAPI configuration.
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
@ConditionalOnProperty(
        prefix = "strata.starter",
        name = "auto-configuration",
        havingValue = "true",
        matchIfMissing = true)
public class StrataOpenApiConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "strataOpenApi")
    OpenAPI strataOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Strata Admin Framework API").version("0.2.1"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    @ConditionalOnMissingBean(name = "strataSystemGroup")
    GroupedOpenApi strataSystemGroup() {
        return GroupedOpenApi.builder()
                .group("system")
                .pathsToMatch("/api/v1/system/**")
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "strataAiGroup")
    GroupedOpenApi strataAiGroup() {
        return GroupedOpenApi.builder()
                .group("ai")
                .pathsToMatch("/api/v1/ai/**")
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "strataOpsGroup")
    GroupedOpenApi strataOpsGroup() {
        return GroupedOpenApi.builder()
                .group("ops")
                .pathsToMatch("/api/v1/ops/**")
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "strataPluginsGroup")
    GroupedOpenApi strataPluginsGroup() {
        return GroupedOpenApi.builder()
                .group("plugins")
                .pathsToMatch("/api/v1/plugins/**")
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "strataDemoGroup")
    GroupedOpenApi strataDemoGroup() {
        return GroupedOpenApi.builder()
                .group("demo")
                .pathsToMatch("/api/v1/demo/**")
                .build();
    }
}
