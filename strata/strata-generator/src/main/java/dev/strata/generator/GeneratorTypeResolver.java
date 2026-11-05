// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import dev.strata.generator.GeneratorRequest.FieldSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Type and naming mapping for codegen: field specs to SQL/Java fragments, plus shared naming
 * helpers.
 *
 * <p>Stateless static utilities; template resource access lives in {@link GeneratorTemplateSupport}.
 */
final class GeneratorTypeResolver {

    private GeneratorTypeResolver() {}

    static String sqlColumnType(FieldSpec field) {
        return switch (field.type()) {
            case STRING -> "VARCHAR";
            case LONG -> "BIGINT";
            case INTEGER -> "INTEGER";
            case DECIMAL -> "DECIMAL(12, 2)";
            case BOOLEAN -> "BOOLEAN";
            case DATETIME -> "TIMESTAMP";
        };
    }

    static String sqlDefault(FieldSpec field) {
        return switch (field.type()) {
            case BOOLEAN -> field.required() ? " DEFAULT TRUE" : "";
            case INTEGER -> field.required() ? " DEFAULT 0" : "";
            default -> "";
        };
    }

    static String entityJavaType(FieldSpec field) {
        return switch (field.type()) {
            case BOOLEAN -> field.required() ? "boolean" : "Boolean";
            case INTEGER -> field.required() ? "int" : "Integer";
            default -> field.type().javaType();
        };
    }

    static String entityInitValue(FieldSpec field) {
        return switch (field.type()) {
            case BOOLEAN -> field.required() ? " = true" : "";
            case INTEGER -> field.required() ? " = 0" : "";
            default -> "";
        };
    }

    static String entityColumnAnnotations(FieldSpec field) {
        List<String> parts = new ArrayList<>();
        String colName = snakeCase(field.name());
        if (!colName.equals(field.name())) {
            parts.add("name = \"" + colName + "\"");
        }
        if (field.required()) {
            parts.add("nullable = false");
        }
        if (field.type() == FieldType.STRING) {
            parts.add("length = " + defaultLength(field));
        }
        if (field.type() == FieldType.DECIMAL) {
            parts.add("precision = 12, scale = 2");
        }
        if (field.unique()) {
            parts.add("unique = true");
        }
        return String.join(", ", parts);
    }

    /** P1-7/P1-8: validation annotations — no @NotNull on primitives, same for create/update. */
    static String validationAnnotations(FieldSpec field) {
        List<String> annos = new ArrayList<>();
        if (field.type() == FieldType.STRING) {
            if (field.required()) annos.add("@NotBlank");
            annos.add("@Size(max = " + defaultLength(field) + ")");
        } else if (field.required() && !isPrimitive(field)) {
            // P1-7: @NotNull only on object/wrapper types, not primitive int/boolean
            annos.add("@NotNull");
        }
        if (field.type() == FieldType.DECIMAL) {
            // P0-3: @DecimalMin only, no duplicate @NotNull (already handled above)
            annos.add("@DecimalMin(\"0.00\")");
        }
        if (field.type() == FieldType.INTEGER && field.required()) {
            annos.add("@Min(0)");
        }
        return String.join(" ", annos);
    }

    private static boolean isPrimitive(FieldSpec field) {
        return (field.type() == FieldType.INTEGER && field.required())
                || (field.type() == FieldType.BOOLEAN && field.required());
    }

    static String requestJavaType(FieldSpec field) {
        return switch (field.type()) {
            case BOOLEAN -> field.required() ? "boolean" : "Boolean";
            case INTEGER -> field.required() ? "int" : "Integer";
            default -> field.type().javaType();
        };
    }

    static String queryJavaType(FieldSpec field) {
        return switch (field.type()) {
            case BOOLEAN -> "Boolean";
            case DATETIME -> "Instant";
            default -> field.type().javaType();
        };
    }

    static String emptyFormValue(FieldSpec field) {
        return switch (field.type()) {
            case STRING, LONG, DATETIME -> "\"\"";
            case INTEGER, DECIMAL -> "0";
            case BOOLEAN -> "false";
        };
    }

    static int defaultLength(FieldSpec field) {
        return field.maxLength() == null || field.maxLength() <= 0 ? 128 : field.maxLength();
    }

    static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    static String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) return value;
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
