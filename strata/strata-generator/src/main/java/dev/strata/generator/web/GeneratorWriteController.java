// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.generator.GeneratorRequest;
import dev.strata.generator.GeneratorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Developer-only disk-writing endpoint for the experimental code generator v1.
 *
 * <p>This controller is assembled only when {@code strata.generator.enabled=true} <strong>and</strong>
 * the active profiles do not include {@code prod}. The read-only {@code preview}/{@code download}
 * endpoints live in {@link GeneratorController}; writing generated files onto the server filesystem
 * must never be reachable in production, even for an authenticated administrator.
 */
@Validated
@RestController
@RequestMapping("/api/v1/generator")
@ConditionalOnProperty(name = "strata.generator.enabled", havingValue = "true")
@Profile("!prod")
public class GeneratorWriteController {

    private final GeneratorService generatorService;

    public GeneratorWriteController(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    /** Writes generated files to disk under the configured {@code strata.generator.output-dir}. */
    @PreAuthorize(Permissions.GENERATOR_CREATE)
    @PostMapping("/write")
    public Result<List<String>> write(@Valid @RequestBody GeneratorRequest request) {
        return Result.ok(generatorService.write(request));
    }
}
