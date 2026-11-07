// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import dev.strata.generator.GeneratorRequest.FieldSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Backend renderers for codegen: generated PostgreSQL migration, Java entity/DTO/query/request/
 * repository/mapper/app-service/controller sources, plus the SQL and menu snippets.
 */
final class GeneratorBackendRenderer {

    private final GeneratorTemplateSupport templates;

    GeneratorBackendRenderer(GeneratorTemplateSupport templates) {
        this.templates = templates;
    }

    // ---------- backend renderers ----------

    String renderMigration(String table, List<FieldSpec> fields) {
        StringBuilder cols = new StringBuilder();
        StringBuilder uniqueConstraints = new StringBuilder();
        Set<String> uniqueCols = new HashSet<>();
        for (FieldSpec field : fields) {
            String column = GeneratorTypeResolver.snakeCase(field.name());
            String type = GeneratorTypeResolver.sqlColumnType(field);
            String length =
                    field.type() == FieldType.STRING ? "(" + GeneratorTypeResolver.defaultLength(field) + ")" : "";
            String nullable = field.required() ? " NOT NULL" : "";
            String def = GeneratorTypeResolver.sqlDefault(field);
            cols.append("    ")
                    .append(column)
                    .append("  ")
                    .append(type)
                    .append(length)
                    .append(nullable)
                    .append(def)
                    .append(",\n");
            if (field.unique() && uniqueCols.add(column)) {
                uniqueConstraints
                        .append("    CONSTRAINT uk_")
                        .append(table)
                        .append("_")
                        .append(column)
                        .append(" UNIQUE (")
                        .append(column)
                        .append("),\n");
            }
        }
        return templates
                .loadTemplate("migration.sql.tpl")
                .replace("{{TABLE}}", table)
                .replace("{{MIGRATION_COLUMNS}}", cols.toString())
                .replace("{{UNIQUE_CONSTRAINTS}}", uniqueConstraints.toString());
    }

    String renderEntity(String pkg, String entity, String table, List<FieldSpec> fields) {
        Set<String> imports = new java.util.TreeSet<>();
        for (FieldSpec f : fields) {
            if (f.type() == FieldType.DECIMAL) imports.add("import java.math.BigDecimal;");
            if (f.type() == FieldType.DATETIME) imports.add("import java.time.Instant;");
        }
        String entityImports = imports.isEmpty() ? "" : String.join("\n", imports) + "\n";

        List<String> fieldLines = new ArrayList<>();
        for (FieldSpec field : fields) {
            String javaType = GeneratorTypeResolver.entityJavaType(field);
            String init = GeneratorTypeResolver.entityInitValue(field);
            String columnAnnos = GeneratorTypeResolver.entityColumnAnnotations(field);
            String anno = columnAnnos.isEmpty() ? "@Column" : "@Column(" + columnAnnos + ")";
            fieldLines.add("    " + anno + "\n    private " + javaType + " " + field.name()
                    + (init.isEmpty() ? "" : init) + ";");
        }
        String entityFields = String.join("\n\n", fieldLines);

        return templates
                .loadTemplate("entity.java.tpl")
                .replace("{{PKG}}", pkg)
                .replace("{{TABLE}}", table)
                .replace("{{E}}", entity)
                .replace("{{ENTITY_IMPORTS}}", entityImports)
                .replace("{{ENTITY_FIELDS}}", entityFields);
    }

    String renderDto(String pkg, String entity, List<FieldSpec> fields) {
        List<String> parts = new ArrayList<>();
        parts.add("String id");
        for (FieldSpec field : fields) {
            String dtoType =
                    field.type() == FieldType.LONG ? "String" : field.type().javaType();
            parts.add(dtoType + " " + field.name());
        }
        String paramsJoined = String.join(", ", parts);
        String singleLine = "public record " + entity + "Dto(" + paramsJoined + ") {}";
        String recordDecl;
        if (singleLine.length() <= 120) {
            recordDecl = singleLine;
        } else {
            String joined = String.join(", ", parts);
            if (joined.length() + 8 <= 120) {
                recordDecl = "public record " + entity + "Dto(\n        " + joined + ") {}";
            } else {
                List<String> indented =
                        parts.stream().map(pt -> "        " + pt).collect(Collectors.toList());
                recordDecl = "public record " + entity + "Dto(\n" + String.join(",\n", indented) + ") {}";
            }
        }

        Set<String> imports = new java.util.TreeSet<>();
        for (FieldSpec f : fields) {
            if (f.type() == FieldType.DECIMAL) imports.add("import java.math.BigDecimal;");
            if (f.type() == FieldType.DATETIME) imports.add("import java.time.Instant;");
        }
        String dtoImports = imports.isEmpty() ? "" : String.join("\n", imports) + "\n\n";

        return templates
                .loadTemplate("dto.java.tpl")
                .replace("{{PKG}}", pkg)
                .replace("{{E}}", entity)
                .replace("{{DTO_IMPORTS}}", dtoImports)
                .replace("{{DTO_RECORD}}", recordDecl);
    }

    String renderQuery(String pkg, String entity, List<FieldSpec> fields) {
        List<String> parts = new ArrayList<>();
        Set<String> imports = new java.util.TreeSet<>();
        for (FieldSpec field : fields) {
            if (!field.queryable()) continue;
            parts.add(GeneratorTypeResolver.queryJavaType(field) + " " + field.name());
            if (field.type() == FieldType.DATETIME) imports.add("import java.time.Instant;");
        }
        parts.add("Integer page");
        parts.add("Integer size");
        String paramsJoined = String.join(", ", parts);
        String singleLine = "public record " + entity + "Query(" + paramsJoined + ") {}";
        String recordDecl;
        if (singleLine.length() <= 120) {
            recordDecl = singleLine;
        } else {
            String joined = String.join(", ", parts);
            if (joined.length() + 8 <= 120) {
                recordDecl = "public record " + entity + "Query(\n        " + joined + ") {}";
            } else {
                List<String> indented =
                        parts.stream().map(pt -> "        " + pt).collect(Collectors.toList());
                recordDecl = "public record " + entity + "Query(\n" + String.join(",\n", indented) + ") {}";
            }
        }
        String queryImports = imports.isEmpty() ? "" : String.join("\n", imports) + "\n\n";

        return templates
                .loadTemplate("query.java.tpl")
                .replace("{{PKG}}", pkg)
                .replace("{{E}}", entity)
                .replace("{{QUERY_IMPORTS}}", queryImports)
                .replace("{{QUERY_RECORD}}", recordDecl);
    }

    String renderCreateRequest(String pkg, String entity, List<FieldSpec> fields) {
        return renderRequestCommon(
                pkg,
                entity,
                fields,
                "{{CREATE_IMPORTS}}",
                "{{CREATE_RECORD}}",
                "create-request.java.tpl",
                "CreateRequest");
    }

    String renderUpdateRequest(String pkg, String entity, List<FieldSpec> fields) {
        return renderRequestCommon(
                pkg,
                entity,
                fields,
                "{{UPDATE_IMPORTS}}",
                "{{UPDATE_RECORD}}",
                "update-request.java.tpl",
                "UpdateRequest");
    }

    private String renderRequestCommon(
            String pkg,
            String entity,
            List<FieldSpec> fields,
            String importPlaceholder,
            String recordPlaceholder,
            String templateName,
            String suffix) {
        List<String> parts = new ArrayList<>();
        Set<String> imports = new java.util.TreeSet<>();
        for (FieldSpec field : fields) {
            String annos = GeneratorTypeResolver.validationAnnotations(field);
            String javaType = GeneratorTypeResolver.requestJavaType(field);
            if (annos.contains("@NotBlank")) imports.add("import jakarta.validation.constraints.NotBlank;");
            if (annos.contains("@NotNull")) imports.add("import jakarta.validation.constraints.NotNull;");
            if (annos.contains("@Size")) imports.add("import jakarta.validation.constraints.Size;");
            if (annos.contains("@DecimalMin")) imports.add("import jakarta.validation.constraints.DecimalMin;");
            if (annos.contains("@Min")) imports.add("import jakarta.validation.constraints.Min;");
            if (field.type() == FieldType.DECIMAL) imports.add("import java.math.BigDecimal;");
            if (field.type() == FieldType.DATETIME) imports.add("import java.time.Instant;");
            parts.add((annos.isEmpty() ? "" : annos + " ") + javaType + " " + field.name());
        }
        String paramsJoined = String.join(", ", parts);
        String recordName = entity + suffix;
        String singleLine = "public record " + recordName + "(" + paramsJoined + ") {}";
        String recordDecl;
        if (singleLine.length() <= 120) {
            recordDecl = singleLine;
        } else {
            String joined = String.join(", ", parts);
            if (joined.length() + 8 <= 120) {
                recordDecl = "public record " + recordName + "(\n        " + joined + ") {}";
            } else {
                List<String> indented =
                        parts.stream().map(pt -> "        " + pt).collect(Collectors.toList());
                recordDecl = "public record " + recordName + "(\n" + String.join(",\n", indented) + ") {}";
            }
        }
        String importBlock = imports.isEmpty() ? "" : String.join("\n", imports) + "\n\n";

        return templates
                .loadTemplate(templateName)
                .replace("{{PKG}}", pkg)
                .replace("{{E}}", entity)
                .replace(importPlaceholder, importBlock)
                .replace(recordPlaceholder, recordDecl);
    }

    String renderRepository(String pkg, String entity, List<FieldSpec> fields) {
        StringBuilder uniqueMethods = new StringBuilder();
        Set<String> repoImports = new java.util.TreeSet<>();
        for (FieldSpec field : fields) {
            if (field.unique()) {
                String capName = GeneratorTypeResolver.capitalize(field.name());
                String paramType = field.type().javaType();
                uniqueMethods
                        .append("\n    boolean existsBy")
                        .append(capName)
                        .append("(")
                        .append(paramType)
                        .append(" ")
                        .append(field.name())
                        .append(");\n");
                uniqueMethods
                        .append("\n    boolean existsBy")
                        .append(capName)
                        .append("AndIdNot(")
                        .append(paramType)
                        .append(" ")
                        .append(field.name())
                        .append(", Long id);\n");
                if (field.type() == FieldType.DECIMAL) repoImports.add("import java.math.BigDecimal;");
                if (field.type() == FieldType.DATETIME) repoImports.add("import java.time.Instant;");
            }
        }
        String repoImportBlock = repoImports.isEmpty() ? "" : String.join("\n", repoImports) + "\n";
        String methods = uniqueMethods.toString().stripTrailing();
        return templates
                .loadTemplate("repository.java.tpl")
                .replace("{{PKG}}", pkg)
                .replace("{{E}}", entity)
                .replace("{{REPO_IMPORTS}}", repoImportBlock)
                .replace("{{UNIQUE_REPO_METHODS}}", methods);
    }

    String renderMapper(String pkg, String entity, String module) {
        return templates
                .loadTemplate("mapper.java.tpl")
                .replace("{{PKG}}", pkg)
                .replace("{{MODULE}}", module)
                .replace("{{E}}", entity);
    }

    String renderAppService(
            String pkg, String entity, String entityLower, String module, String perm, List<FieldSpec> fields) {
        StringBuilder specs = new StringBuilder();
        StringBuilder createValidation = new StringBuilder();
        StringBuilder updateValidation = new StringBuilder();
        for (FieldSpec field : fields) {
            if (!field.queryable()) continue;
            String op = field.type() == FieldType.STRING ? "like" : "eq";
            specs.append("                .")
                    .append(op)
                    .append("(\"")
                    .append(field.name())
                    .append("\", query.")
                    .append(field.name())
                    .append("())\n");
        }
        for (FieldSpec field : fields) {
            if (field.unique()) {
                String capName = GeneratorTypeResolver.capitalize(field.name());
                String typeCast = "";
                if (field.type() != FieldType.STRING) {
                    typeCast = "(" + field.type().javaType() + ") ";
                }
                createValidation
                        .append("        if (")
                        .append(entityLower)
                        .append("Repository.existsBy")
                        .append(capName)
                        .append("(request.")
                        .append(field.name())
                        .append("())) {\n")
                        .append("            throw new BusinessException(ErrorCode.CONFLICT, \"")
                        .append(capName)
                        .append(" already exists\");\n")
                        .append("        }\n");
                updateValidation
                        .append("        if (")
                        .append(entityLower)
                        .append("Repository.existsBy")
                        .append(capName)
                        .append("AndIdNot(request.")
                        .append(field.name())
                        .append("(), id)) {\n")
                        .append("            throw new BusinessException(ErrorCode.CONFLICT, \"")
                        .append(capName)
                        .append(" already exists\");\n")
                        .append("        }\n");
            }
        }
        return templates
                .loadTemplate("app-service.java.tpl")
                .replace("{{PKG}}", pkg)
                .replace("{{MODULE}}", module)
                .replace("{{E}}", entity)
                .replace("{{EL}}", entityLower)
                .replace("{{PERM}}", perm)
                .replace("{{SPEC_LINES}}", specs.toString())
                .replace("{{CREATE_VALIDATION}}", createValidation.toString())
                .replace("{{UPDATE_VALIDATION}}", updateValidation.toString());
    }

    String renderController(
            String pkg,
            String entity,
            String entityLower,
            String module,
            String resource,
            String perm,
            List<FieldSpec> fields) {
        StringBuilder params = new StringBuilder();
        StringBuilder ctorArgs = new StringBuilder();
        Set<String> controllerImports = new java.util.TreeSet<>();
        for (FieldSpec field : fields) {
            if (!field.queryable()) continue;
            String javaType = GeneratorTypeResolver.queryJavaType(field);
            // P1-11: @DateTimeFormat for Instant query params
            if (field.type() == FieldType.DATETIME) {
                params.append("            @RequestParam(required = false) @DateTimeFormat(iso ="
                                + " DateTimeFormat.ISO.DATE_TIME) Instant ")
                        .append(field.name())
                        .append(",\n");
                controllerImports.add("import org.springframework.format.annotation.DateTimeFormat;\n");
                controllerImports.add("import java.time.Instant;\n");
            } else {
                params.append("            @RequestParam(required = false) ")
                        .append(javaType)
                        .append(" ")
                        .append(field.name())
                        .append(",\n");
            }
            ctorArgs.append(field.name()).append(", ");
        }
        ctorArgs.append("page, size");
        String importBlock = controllerImports.isEmpty() ? "" : String.join("", controllerImports);

        return templates
                .loadTemplate("controller.java.tpl")
                .replace("{{PKG}}", pkg)
                .replace("{{MODULE}}", module)
                .replace("{{E}}", entity)
                .replace("{{EL}}", entityLower)
                .replace("{{RESOURCE}}", resource)
                .replace("{{PERM}}", perm)
                .replace("{{CONTROLLER_IMPORTS}}", importBlock)
                .replace("{{CONTROLLER_PARAMS}}", params.toString())
                .replace("{{QUERY_CONSTRUCTOR_ARGS}}", ctorArgs.toString());
    }

    // ---------- SQL / menu renderers ----------

    String renderMenuSnippet(String entity, String module, String resource, String perm) {
        return templates
                .loadTemplate("menu-snippet.json.tpl")
                .replace("{{RESOURCE}}", resource)
                .replace("{{MODULE}}", module)
                .replace("{{E}}", entity)
                .replace("{{PERM}}", perm)
                .replace("\r\n", "\n");
    }

    String renderRouteSnippet(String entity, String module, String resource, String perm) {
        return templates
                .loadTemplate("route-snippet.ts.tpl")
                .replace("{{E}}", entity)
                .replace("{{MODULE}}", module)
                .replace("{{RESOURCE}}", resource)
                .replace("{{PERM}}", perm)
                .replace("\r\n", "\n");
    }
}
