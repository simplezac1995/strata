// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import dev.strata.generator.GeneratorRequest.FieldSpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Codegen v2: single table → backend quartet + frontend list/form, all in Strata golden-path flavor.
 *
 * <p>Templates are externalised under {@code classpath:/templates/} and rendered via {@link
 * String#replace} — no heavy template engine. Package declarations are driven by {@code
 * basePackage} (default {@code dev.strata.system}). Menu seed is emitted as a JSON snippet aligned
 * with {@code menus.json} (no id — the runtime assigns snowflake IDs on load).
 *
 * <p>Rendering lives in {@link GeneratorBackendRenderer} and {@link GeneratorFrontendRenderer}; type
 * and naming mapping in {@link GeneratorTypeResolver}.
 */
@Service
@ConditionalOnProperty(name = "strata.generator.enabled", havingValue = "true")
public class GeneratorService {

    private static final Logger log = LoggerFactory.getLogger(GeneratorService.class);

    private final GeneratorProperties properties;
    private final GeneratorTemplateSupport templates = new GeneratorTemplateSupport();
    private final GeneratorBackendRenderer backendRenderer = new GeneratorBackendRenderer(templates);
    private final GeneratorFrontendRenderer frontendRenderer = new GeneratorFrontendRenderer(templates);

    public GeneratorService(GeneratorProperties properties) {
        this.properties = properties;
    }

    public List<GeneratorFile> preview(GeneratorRequest request) {
        return buildFiles(request);
    }

    public byte[] download(GeneratorRequest request) {
        List<GeneratorFile> files = buildFiles(request);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (GeneratorFile file : files) {
                zip.putNextEntry(new ZipEntry(file.path()));
                zip.write(file.content().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException ex) {
            log.error("Failed to build codegen zip: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to build zip", ex);
        }
        return buffer.toByteArray();
    }

    /** Writes the generated files onto disk under {@code strata.generator.output-dir}. */
    public List<String> write(GeneratorRequest request) {
        Path root = Paths.get(properties.outputDir()).toAbsolutePath().normalize();
        List<String> written = new ArrayList<>();
        try {
            Files.createDirectories(root);
            for (GeneratorFile file : buildFiles(request)) {
                Path target = root.resolve(file.path()).normalize();
                if (!target.startsWith(root)) {
                    throw new IllegalArgumentException("Generated path escapes the output dir: " + file.path());
                }
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.content(), StandardCharsets.UTF_8);
                written.add(target.toString());
            }
        } catch (IOException ex) {
            log.error("Failed to write generated files: {}", ex.getMessage(), ex);
            throw new UncheckedIOException("Failed to write generated files", ex);
        }
        return written;
    }

    // ---------- file assembly ----------

    private List<GeneratorFile> buildFiles(GeneratorRequest request) {
        String entity = GeneratorTypeResolver.capitalize(request.entityName());
        String entityLower = GeneratorTypeResolver.lowerFirst(entity);
        String table = request.tableName().toLowerCase(Locale.ROOT);
        String resource = request.resource().toLowerCase(Locale.ROOT);
        String module = request.module().toLowerCase(Locale.ROOT);
        // Permission base: module + ":" + entityName.toLowerCase() (all lowercase, P1-10)
        String perm = module + ":" + request.entityName().toLowerCase(Locale.ROOT);
        String basePackage = request.basePackage();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        List<FieldSpec> fields = request.fields();

        List<GeneratorFile> files = new ArrayList<>();
        // PostgreSQL is the only supported runtime database and migration dialect.
        files.add(new GeneratorFile(
                "postgresql/V" + timestamp + "__create_" + table + ".sql",
                backendRenderer.renderMigration(table, fields)));
        files.add(new GeneratorFile(
                "api/entity/" + entity + ".java", backendRenderer.renderEntity(basePackage, entity, table, fields)));
        files.add(new GeneratorFile(
                "api/dto/" + entity + "Dto.java", backendRenderer.renderDto(basePackage, entity, fields)));
        files.add(new GeneratorFile(
                "api/dto/" + entity + "Query.java", backendRenderer.renderQuery(basePackage, entity, fields)));
        files.add(new GeneratorFile(
                "api/request/" + entity + "CreateRequest.java",
                backendRenderer.renderCreateRequest(basePackage, entity, fields)));
        files.add(new GeneratorFile(
                "api/request/" + entity + "UpdateRequest.java",
                backendRenderer.renderUpdateRequest(basePackage, entity, fields)));
        files.add(new GeneratorFile(
                "biz/repository/" + entity + "Repository.java",
                backendRenderer.renderRepository(basePackage, entity, fields)));
        files.add(new GeneratorFile(
                "biz/" + module + "/" + entity + "Mapper.java",
                backendRenderer.renderMapper(basePackage, entity, module)));
        files.add(new GeneratorFile(
                "biz/" + module + "/" + entity + "AppService.java",
                backendRenderer.renderAppService(basePackage, entity, entityLower, module, perm, fields)));
        files.add(new GeneratorFile(
                "biz/" + module + "/" + entity + "Controller.java",
                backendRenderer.renderController(basePackage, entity, entityLower, module, resource, perm, fields)));
        files.add(new GeneratorFile("frontend/shared/types.ts", frontendRenderer.renderFrontendTypes(entity, fields)));
        files.add(new GeneratorFile(
                "frontend/shared/api/" + module + ".ts",
                frontendRenderer.renderFrontendApi(entity, entityLower, module, resource, fields)));
        files.add(new GeneratorFile(
                "frontend/pages/" + module + "/" + entity + "List.tsx",
                frontendRenderer.renderFrontendPage(entity, entityLower, module, resource, perm, fields)));
        files.add(new GeneratorFile(
                "frontend/route-snippet.ts", backendRenderer.renderRouteSnippet(entity, module, resource, perm)));
        files.add(new GeneratorFile(
                "menu-snippet.json", backendRenderer.renderMenuSnippet(entity, module, resource, perm)));
        return files;
    }
}
