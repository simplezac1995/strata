// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.generator.GeneratorFile;
import dev.strata.generator.GeneratorRequest;
import dev.strata.generator.GeneratorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/generator")
@ConditionalOnProperty(name = "strata.generator.enabled", havingValue = "true")
public class GeneratorController {

    private final GeneratorService generatorService;

    public GeneratorController(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PreAuthorize(Permissions.GENERATOR_LIST)
    @PostMapping("/preview")
    public Result<List<GeneratorFile>> preview(@Valid @RequestBody GeneratorRequest request) {
        return Result.ok(generatorService.preview(request));
    }

    @PreAuthorize(Permissions.GENERATOR_LIST)
    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@Valid @RequestBody GeneratorRequest request) {
        byte[] zip = generatorService.download(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"strata-codegen.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }
}
