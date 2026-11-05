// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Codegen request: one table → backend quartet + frontend list/form in Strata golden-path flavor. */
public record GeneratorRequest(
        String module,
        @NotBlank(message = "tableName is required") String tableName,
        @NotBlank(message = "entityName is required") String entityName,
        String resource,
        String menuLabel,
        String basePackage,
        List<FieldSpec> fields) {

    public GeneratorRequest {
        if (module == null || module.isBlank()) module = "demo";
        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException("entityName is required");
        }
        if (resource == null || resource.isBlank()) resource = pluralize(entityName);
        if (menuLabel == null || menuLabel.isBlank()) menuLabel = capitalize(entityName) + " List";
        if (basePackage == null || basePackage.isBlank()) basePackage = "dev.strata.system";
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    private static String pluralize(String name) {
        if (name == null || name.isBlank()) return "items";
        String lower = name.toLowerCase();
        if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("ch")) return lower + "es";
        if (lower.endsWith("y") && lower.length() > 1 && !isVowel(lower.charAt(lower.length() - 2))) {
            return lower.substring(0, lower.length() - 1) + "ies";
        }
        return lower + "s";
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /** Field spec for codegen. */
    public record FieldSpec(
            String name, FieldType type, boolean required, Integer maxLength, boolean queryable, boolean unique) {}
}
