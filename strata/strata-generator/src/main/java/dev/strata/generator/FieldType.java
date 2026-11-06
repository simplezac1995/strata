// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

/** Field types supported by codegen v1. */
public enum FieldType {
    STRING("String", "string", "VARCHAR", "z.string()"),
    LONG("Long", "string", "BIGINT", "z.string().min(1)"),
    INTEGER("Integer", "number", "INT", "z.number().int()"),
    DECIMAL("BigDecimal", "number", "DECIMAL(12, 2)", "z.number()"),
    BOOLEAN("Boolean", "boolean", "BOOLEAN", "z.boolean()"),
    DATETIME("Instant", "string", "TIMESTAMP", "z.string()");

    private final String javaType;
    private final String tsType;
    private final String columnType;
    private final String zodType;

    FieldType(String javaType, String tsType, String columnType, String zodType) {
        this.javaType = javaType;
        this.tsType = tsType;
        this.columnType = columnType;
        this.zodType = zodType;
    }

    public String javaType() {
        return javaType;
    }

    public String tsType() {
        return tsType;
    }

    public String columnType() {
        return columnType;
    }

    public String zodType() {
        return zodType;
    }
}
