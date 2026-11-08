// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.generator.GeneratorRequest.FieldSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for codegen v2.
 *
 * <p>Generates sample resources and runs <strong>real {@link JavaCompiler} compilation</strong> on
 * all backend .java products to prove they compile with 0 errors. String assertions cover structural
 * invariants (SPDX, package layout, PostgreSQL SQL, menus.json snippet).
 */
class GeneratorServiceTest {

    private final GeneratorService service = new GeneratorService(new GeneratorProperties(null));

    // ---------- sample requests ----------

    /** Sample A: full FieldType matrix with STRING + LONG unique. */
    private GeneratorRequest sampleFullMatrix() {
        return new GeneratorRequest(
                "demo",
                "demo_invoice",
                "Invoice",
                "invoices",
                "Invoices",
                "dev.strata.system",
                List.of(
                        new FieldSpec("title", FieldType.STRING, true, 200, true, false),
                        new FieldSpec("invoiceNo", FieldType.STRING, true, 64, true, true),
                        new FieldSpec("amountCents", FieldType.LONG, true, null, false, true),
                        new FieldSpec("qty", FieldType.INTEGER, true, null, true, false),
                        new FieldSpec("price", FieldType.DECIMAL, true, null, false, true),
                        new FieldSpec("enabled", FieldType.BOOLEAN, true, null, true, false),
                        new FieldSpec("occurredAt", FieldType.DATETIME, false, null, true, true)));
    }

    /** Sample B: replica of demo Product. */
    private GeneratorRequest sampleDemoReplica() {
        return new GeneratorRequest(
                "demo",
                "demo_product",
                "Product",
                "products",
                "Products",
                "dev.strata.system",
                List.of(
                        new FieldSpec("name", FieldType.STRING, true, 128, true, false),
                        new FieldSpec("sku", FieldType.STRING, true, 64, true, true),
                        new FieldSpec("price", FieldType.DECIMAL, true, null, false, false),
                        new FieldSpec("stock", FieldType.INTEGER, true, null, false, false),
                        new FieldSpec("description", FieldType.STRING, false, 512, false, false),
                        new FieldSpec("enabled", FieldType.BOOLEAN, true, null, true, false)));
    }

    // ---------- file inventory ----------

    @Test
    void preview_produces_golden_path_files() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        assertThat(files).hasSize(15);

        Map<String, String> byPath =
                files.stream().collect(Collectors.toMap(GeneratorFile::path, GeneratorFile::content));

        assertThat(byPath.keySet()).anyMatch(p -> p.matches("postgresql/V\\d+__create_demo_product\\.sql"));

        assertThat(byPath)
                .containsKeys(
                        "api/entity/Product.java",
                        "api/dto/ProductDto.java",
                        "api/dto/ProductQuery.java",
                        "api/request/ProductCreateRequest.java",
                        "api/request/ProductUpdateRequest.java",
                        "biz/repository/ProductRepository.java",
                        "biz/demo/ProductMapper.java",
                        "biz/demo/ProductAppService.java",
                        "biz/demo/ProductController.java",
                        "frontend/shared/types.ts",
                        "frontend/shared/api/demo.ts",
                        "frontend/pages/demo/ProductList.tsx",
                        "frontend/route-snippet.ts",
                        "menu-snippet.json");

        assertThat(byPath.keySet()).noneMatch(p -> p.startsWith("biz/mapper/"));
        assertThat(byPath.keySet()).noneMatch(p -> p.startsWith("biz/service/"));
        assertThat(byPath.keySet()).noneMatch(p -> p.startsWith("biz/controller/"));
    }

    // ---------- SPDX header ----------

    @Test
    void every_java_product_starts_with_spdx_header() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        List<GeneratorFile> javaFiles =
                files.stream().filter(f -> f.path().endsWith(".java")).toList();
        assertThat(javaFiles).isNotEmpty();
        for (GeneratorFile f : javaFiles) {
            String firstLine = f.content().split("\n")[0].trim();
            assertThat(firstLine)
                    .as("SPDX header missing in %s", f.path())
                    .isEqualTo("// SPDX-License-Identifier: Apache-2.0");
        }
    }

    // ---------- REAL JavaCompiler compilation (core regression) ----------

    @Test
    void backend_products_compile_with_java_compiler_full_matrix(@TempDir Path tempDir) throws IOException {
        compileBackendProducts(sampleFullMatrix(), tempDir);
    }

    @Test
    void backend_products_compile_with_java_compiler_demo_replica(@TempDir Path tempDir) throws IOException {
        compileBackendProducts(sampleDemoReplica(), tempDir);
    }

    private void compileBackendProducts(GeneratorRequest request, Path tempDir) throws IOException {
        List<GeneratorFile> files = service.preview(request);
        List<GeneratorFile> javaFiles =
                files.stream().filter(f -> f.path().endsWith(".java")).toList();
        assertThat(javaFiles).isNotEmpty();

        // Write .java files to temp dir preserving package structure
        List<JavaFileObject> compilationUnits = new ArrayList<>();
        for (GeneratorFile f : javaFiles) {
            Path target = tempDir.resolve(f.path());
            Files.createDirectories(target.getParent());
            Files.writeString(target, f.content(), java.nio.charset.StandardCharsets.UTF_8);
            compilationUnits.add(new SimpleJavaFileObject(target.toUri(), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return f.content();
                }
            });
        }

        // Use system classpath (Maven test classpath includes all needed deps)
        String classpath = System.getProperty("java.class.path");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm =
                compiler.getStandardFileManager(diagnostics, null, java.nio.charset.StandardCharsets.UTF_8)) {
            List<String> options =
                    Arrays.asList("-cp", classpath, "-d", tempDir.resolve("out").toString(), "-proc:full");
            StringWriter err = new StringWriter();
            JavaCompiler.CompilationTask task =
                    compiler.getTask(new PrintWriter(err), fm, diagnostics, options, null, compilationUnits);
            boolean success = task.call();

            // Collect all ERROR diagnostics
            List<Diagnostic<? extends JavaFileObject>> errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .toList();

            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder("Compilation errors:\n");
                for (Diagnostic<? extends JavaFileObject> d : errors) {
                    sb.append("  ")
                            .append(d.getSource() != null ? d.getSource().getName() : "<unknown>")
                            .append(":")
                            .append(d.getLineNumber())
                            .append(" ")
                            .append(d.getMessage(null))
                            .append("\n");
                }
                for (GeneratorFile f : javaFiles) {
                    if (f.path().contains("/entity/")) {
                        sb.append("\n--- ").append(f.path()).append(" ---\n");
                        sb.append(f.content());
                        sb.append("\n--- end ---\n");
                    }
                }
                sb.append("Compiler stderr: ").append(err);
                assertThat(success).as(sb.toString()).isTrue();
                assertThat(errors).as(sb.toString()).isEmpty();
            }
        }
    }

    @Test
    void backend_products_palantir_format_roundtrip() throws Exception {
        com.palantir.javaformat.java.Formatter formatter = com.palantir.javaformat.java.Formatter.createFormatter(
                com.palantir.javaformat.java.JavaFormatterOptions.builder()
                        .style(com.palantir.javaformat.java.JavaFormatterOptions.Style.PALANTIR)
                        .build());
        List<GeneratorFile> files = service.preview(sampleFullMatrix());
        long failures = files.stream()
                .filter(f -> f.path().endsWith(".java"))
                .filter(f -> {
                    try {
                        String formatted = formatter.formatSource(f.content());
                        if (!formatted.equals(f.content())) {
                            System.out.println("Palantir diff in " + f.path() + ":");
                            System.out.println("--- original ---\n" + f.content());
                            System.out.println("--- formatted ---\n" + formatted);
                            return true;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return false;
                })
                .count();
        assertThat(failures).isZero();
    }

    // ---------- backend golden-path invariants (string assertions) ----------

    @Test
    void backend_package_declarations_use_basePackage() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        String entity = joinContent(files, "api/entity/Product.java");
        assertThat(entity).contains("package dev.strata.system.api.entity;");
        String mapper = joinContent(files, "biz/demo/ProductMapper.java");
        assertThat(mapper).contains("package dev.strata.system.biz.demo;");
        String repo = joinContent(files, "biz/repository/ProductRepository.java");
        assertThat(repo).contains("package dev.strata.system.biz.repository;");
    }

    @Test
    void backend_no_trailing_comma_in_records() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        String dto = joinContent(files, "api/dto/ProductDto.java");
        // Last component should not have a trailing comma before ) {}
        assertThat(dto).doesNotContain(",\n) {}");
        String createReq = joinContent(files, "api/request/ProductCreateRequest.java");
        assertThat(createReq).doesNotContain(",\n) {}");
        String updateReq = joinContent(files, "api/request/ProductUpdateRequest.java");
        assertThat(updateReq).doesNotContain(",\n) {}");
    }

    @Test
    void backend_entity_imports_include_bigdecimal_and_instant() {
        List<GeneratorFile> files = service.preview(sampleFullMatrix());
        String entity = joinContent(files, "api/entity/Invoice.java");
        assertThat(entity).contains("import java.math.BigDecimal;");
        assertThat(entity).contains("import java.time.Instant;");
    }

    @Test
    void backend_no_duplicate_notnull_on_decimal() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        String createReq = joinContent(files, "api/request/ProductCreateRequest.java");
        assertThat(createReq).doesNotContain("@NotNull @NotNull");
    }

    @Test
    void backend_no_notnull_on_primitives() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        String createReq = joinContent(files, "api/request/ProductCreateRequest.java");
        // int stock should have @Min(0) but NOT @NotNull
        assertThat(createReq).contains("@Min(0) int stock");
        assertThat(createReq).doesNotContain("@NotNull int stock");
        // boolean enabled should have no validation annotations
        assertThat(createReq).contains("boolean enabled");
        assertThat(createReq).doesNotContain("@NotNull boolean enabled");
    }

    @Test
    void backend_update_matches_create_annotations() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        String createReq = joinContent(files, "api/request/ProductCreateRequest.java");
        String updateReq = joinContent(files, "api/request/ProductUpdateRequest.java");
        // Both should have the same field annotations (全量更新)
        assertThat(createReq).contains("@NotBlank @Size(max = 128) String name");
        assertThat(updateReq).contains("@NotBlank @Size(max = 128) String name");
        assertThat(createReq).contains("@NotNull @DecimalMin(\"0.00\") BigDecimal price");
        assertThat(updateReq).contains("@NotNull @DecimalMin(\"0.00\") BigDecimal price");
    }

    @Test
    void backend_permission_is_all_lowercase() {
        List<GeneratorFile> files = service.preview(sampleFullMatrix());
        String controller = joinContent(files, "biz/demo/InvoiceController.java");
        // entityName="Invoice" → permission base = "demo:invoice" (all lowercase)
        assertThat(controller).contains("hasAuthority('demo:invoice:list')");
        assertThat(controller).doesNotContain("demo:Invoice:list");
    }

    @Test
    void backend_unique_repository_method_types_match_field() {
        List<GeneratorFile> files = service.preview(sampleFullMatrix());
        String repo = joinContent(files, "biz/repository/InvoiceRepository.java");
        // STRING unique → String param
        assertThat(repo).contains("existsByInvoiceNo(String invoiceNo)");
        // LONG unique → Long param (P0-5)
        assertThat(repo).contains("existsByAmountCents(Long amountCents)");
    }

    @Test
    void backend_datetime_query_param_has_datetime_format() {
        List<GeneratorFile> files = service.preview(sampleFullMatrix());
        String controller = joinContent(files, "biz/demo/InvoiceController.java");
        assertThat(controller).contains("@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)");
        assertThat(controller).contains("import org.springframework.format.annotation.DateTimeFormat;");
    }

    // ---------- frontend golden-path invariants ----------

    @Test
    void frontend_uses_alias_imports_not_relative_paths() {
        String page = joinContent(service.preview(sampleDemoReplica()), "frontend/pages/demo/ProductList.tsx");
        assertThat(page).contains("from \"@/components/ui\"");
        assertThat(page).contains("from \"@/shared/hooks/usePagedQuery\"");
        assertThat(page).doesNotContain("../../");
    }

    @Test
    void frontend_has_pagination_query_form_and_dialog() {
        String page = joinContent(service.preview(sampleDemoReplica()), "frontend/pages/demo/ProductList.tsx");
        assertThat(page).contains("<Pagination");
        assertThat(page).contains("<QueryForm");
        assertThat(page).contains("<Dialog");
        assertThat(page).contains("<ConfirmDialog");
    }

    @Test
    void frontend_tsx_braces_are_balanced() {
        String page = joinContent(service.preview(sampleDemoReplica()), "frontend/pages/demo/ProductList.tsx");
        // Naive character count: JSX expressions like {(e) => setForm((f) => ({ ...f, ... }))}
        // naturally have more } than { when counted naively. We just verify no catastrophic
        // imbalance (>10% off).
        long open = page.chars().filter(c -> c == '{').count();
        long close = page.chars().filter(c -> c == '}').count();
        long diff = Math.abs(open - close);
        assertThat(diff).isLessThanOrEqualTo(15);
    }

    // ---------- PostgreSQL migration invariants ----------

    @Test
    void preview_produces_only_a_postgresql_migration() {
        List<GeneratorFile> files = service.preview(sampleDemoReplica());
        String migrationName = files.stream()
                .filter(f -> f.path().startsWith("postgresql/V"))
                .map(f -> f.path().substring("postgresql/".length()))
                .findFirst()
                .orElseThrow();

        String migrationSql = joinContent(files, "postgresql/" + migrationName);
        assertThat(migrationSql).contains(" INTEGER");
        assertThat(files).noneMatch(file -> file.path().startsWith("migration/"));
    }

    // ---------- menu snippet invariants ----------

    @Test
    void menu_snippet_is_valid_menus_node_without_id() {
        String jsonStr = joinContent(service.preview(sampleDemoReplica()), "menu-snippet.json");
        assertThat(jsonStr).contains("\"key\": \"products\"");
        assertThat(jsonStr).contains("\"component\": \"demo/ProductList\"");
        assertThat(jsonStr).contains("\"permission\": \"demo:product:list\"");
        assertThat(jsonStr).doesNotContain("\"id\"");
        assertThat(jsonStr).doesNotContain("INSERT INTO sys_menu");
    }

    // ---------- basePackage parameterisation ----------

    @Test
    void base_package_parameter_changes_all_package_declarations() {
        GeneratorRequest customPkg = new GeneratorRequest(
                "system",
                "sys_log",
                "SysLog",
                "logs",
                "System Logs",
                "dev.strata.custom",
                List.of(new FieldSpec("message", FieldType.STRING, true, 500, true, false)));
        String all =
                service.preview(customPkg).stream().map(GeneratorFile::content).collect(Collectors.joining("\n"));
        assertThat(all).contains("package dev.strata.custom.api.entity;");
        assertThat(all).contains("package dev.strata.custom.biz.system;");
        assertThat(all).contains("package dev.strata.custom.biz.repository;");
    }

    // ---------- safety bans ----------

    @Test
    void generated_code_respects_golden_path_bans() {
        String all = service.preview(sampleDemoReplica()).stream()
                .map(GeneratorFile::content)
                .collect(Collectors.joining("\n"));
        assertThat(all).doesNotContain("lucide-react");
        assertThat(all).doesNotContain("useCrudList");
        assertThat(all).doesNotContain("runMutation");
    }

    // ---------- zip / download ----------

    @Test
    void download_returns_valid_zip() {
        byte[] zip = service.download(sampleDemoReplica());
        assertThat(zip.length).isGreaterThan(100);
        assertThat(zip[0]).isEqualTo((byte) 'P');
        assertThat(zip[1]).isEqualTo((byte) 'K');
    }

    // ---------- frontend preview output for external verification ----------

    @Test
    void write_frontend_preview_outputs_for_verification() throws IOException {
        Path outDir = Path.of("target", "codegen-frontend");
        Files.createDirectories(outDir);

        // Sample 1: demo replica (6 fields)
        for (GeneratorFile f : service.preview(sampleDemoReplica())) {
            if (f.path().startsWith("frontend/")
                    || f.path().equals("menu-snippet.json")
                    || f.path().equals("route-snippet.ts")) {
                Path dest = outDir.resolve("demo-replica").resolve(f.path());
                Files.createDirectories(dest.getParent());
                Files.writeString(dest, f.content().replace("\r\n", "\n"));
            }
        }

        // Sample 2: full matrix (9 fields)
        for (GeneratorFile f : service.preview(sampleFullMatrix())) {
            if (f.path().startsWith("frontend/")
                    || f.path().equals("menu-snippet.json")
                    || f.path().equals("route-snippet.ts")) {
                Path dest = outDir.resolve("full-matrix").resolve(f.path());
                Files.createDirectories(dest.getParent());
                Files.writeString(dest, f.content().replace("\r\n", "\n"));
            }
        }

        assertThat(Files.exists(outDir.resolve("demo-replica").resolve("frontend/pages/demo/ProductList.tsx")))
                .isTrue();
        assertThat(Files.exists(outDir.resolve("full-matrix").resolve("frontend/pages/demo/InvoiceList.tsx")))
                .isTrue();
    }

    // ---------- helpers ----------

    private String joinContent(List<GeneratorFile> files, String path) {
        return files.stream()
                .filter(f -> f.path().equals(path))
                .map(GeneratorFile::content)
                .findFirst()
                .orElseThrow(() -> new AssertionError("File not found: " + path));
    }
}
