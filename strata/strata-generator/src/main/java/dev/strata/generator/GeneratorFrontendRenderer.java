// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import dev.strata.generator.GeneratorRequest.FieldSpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Frontend renderers for codegen: shared TypeScript types, the module API client and the
 * list/form page, plus the form-snippet helpers they use.
 */
final class GeneratorFrontendRenderer {

    private final GeneratorTemplateSupport templates;

    GeneratorFrontendRenderer(GeneratorTemplateSupport templates) {
        this.templates = templates;
    }

    // ---------- frontend renderers ----------

    String renderFrontendTypes(String entity, List<FieldSpec> fields) {
        StringBuilder props = new StringBuilder();
        for (FieldSpec field : fields) {
            String tsType =
                    field.type() == FieldType.LONG ? "string" : field.type().tsType();
            props.append("  ").append(field.name()).append(": ").append(tsType);
            if (!field.required() && field.type() != FieldType.BOOLEAN) props.append(" | null");
            props.append(";\n");
        }
        return templates
                .loadTemplate("frontend-types.ts.tpl")
                .replace("{{E}}", entity)
                .replace("{{TS_DTO_FIELDS}}", props.toString())
                .replace("\r\n", "\n");
    }

    String renderFrontendApi(
            String entity, String entityLower, String module, String resource, List<FieldSpec> fields) {
        StringBuilder payloadFields = new StringBuilder();
        StringBuilder queryFields = new StringBuilder();
        StringBuilder apiParams = new StringBuilder();
        for (FieldSpec field : fields) {
            String tsType =
                    field.type() == FieldType.LONG ? "string" : field.type().tsType();
            payloadFields.append("  ").append(field.name()).append(": ").append(tsType);
            if (!field.required() && field.type() != FieldType.BOOLEAN) payloadFields.append(" | null");
            payloadFields.append(";\n");
            if (field.queryable()) {
                if (field.type() == FieldType.BOOLEAN) {
                    queryFields.append("  ").append(field.name()).append("?: boolean;\n");
                } else {
                    queryFields
                            .append("  ")
                            .append(field.name())
                            .append("?: ")
                            .append(tsType)
                            .append(";\n");
                }
                apiParams
                        .append("    if (query.")
                        .append(field.name())
                        .append(" !== undefined) params.set(\"")
                        .append(field.name())
                        .append("\", String(query.")
                        .append(field.name())
                        .append("));\n");
            }
        }
        return templates
                .loadTemplate("frontend-api.ts.tpl")
                .replace("{{E}}", entity)
                .replace("{{EL}}", entityLower)
                .replace("{{MODULE}}", module)
                .replace("{{RESOURCE}}", resource)
                .replace("{{TS_PAYLOAD_FIELDS}}", payloadFields.toString())
                .replace("{{TS_QUERY_FIELDS}}", queryFields.toString())
                .replace("{{API_QUERY_PARAMS}}", apiParams.toString())
                .replace("\r\n", "\n");
    }

    String renderFrontendPage(
            String entity, String entityLower, String module, String resource, String perm, List<FieldSpec> fields) {
        StringBuilder filterStateDecls = new StringBuilder();
        StringBuilder filterQueryKeysInit = new StringBuilder();
        StringBuilder filterQueryKeysSearch = new StringBuilder();
        StringBuilder filterApiArgs = new StringBuilder();
        StringBuilder filterReset = new StringBuilder();
        StringBuilder filterFields = new StringBuilder();
        StringBuilder columns = new StringBuilder();
        StringBuilder formFields = new StringBuilder();
        List<String> emptyPairs = new ArrayList<>();
        List<String> editPairs = new ArrayList<>();
        boolean first = true;

        for (FieldSpec field : fields) {
            String name = field.name();
            String emptyVal = GeneratorTypeResolver.emptyFormValue(field);

            if (field.type() == FieldType.BOOLEAN) {
                formFields
                        .append("        <Select\n")
                        .append("          label={t(\"common.status\")}\n")
                        .append("          value={form.")
                        .append(name)
                        .append(" ? \"true\" : \"false\"}\n")
                        .append(renderFormOnChange(name, "e.target.value === \"true\""))
                        .append("          options={[\n")
                        .append("            { value: \"true\", label: t(\"common.enabled\") },\n")
                        .append("            { value: \"false\", label: t(\"common.disabled\") },\n")
                        .append("          ]}\n")
                        .append("        />\n");
            } else if (field.type() == FieldType.INTEGER || field.type() == FieldType.DECIMAL) {
                formFields
                        .append("        <TextField\n")
                        .append("          label=\"")
                        .append(name)
                        .append("\"\n")
                        .append("          type=\"number\"\n")
                        .append("          value={String(form.")
                        .append(name)
                        .append(")}\n")
                        .append(renderFormOnChange(name, "Number(e.target.value)"))
                        .append("        />\n");
            } else {
                formFields
                        .append("        <TextField\n")
                        .append("          label=\"")
                        .append(name)
                        .append("\"\n")
                        .append("          value={form.")
                        .append(name)
                        .append("}\n")
                        .append(renderFormOnChange(name, "e.target.value"))
                        .append("        />\n");
            }

            emptyPairs.add(name + ": " + emptyVal);
            first = false;
            editPairs.add(name + ": row." + name);

            if (field.type() == FieldType.BOOLEAN) {
                columns.append("    {\n")
                        .append("      key: \"")
                        .append(name)
                        .append("\",\n")
                        .append("      title: t(\"common.status\"),\n")
                        .append("      render: (row: ")
                        .append(entity)
                        .append("Dto) => <StatusChip enabled={row.")
                        .append(name)
                        .append("} />,\n")
                        .append("    },\n");
            } else {
                columns.append("    { key: \"")
                        .append(name)
                        .append("\", title: \"")
                        .append(name)
                        .append("\" },\n");
            }

            if (field.queryable()) {
                filterStateDecls
                        .append("  const [filter")
                        .append(GeneratorTypeResolver.capitalize(name))
                        .append(", setFilter")
                        .append(GeneratorTypeResolver.capitalize(name))
                        .append("] = useState(\"");
                if (field.type() == FieldType.BOOLEAN) {
                    filterStateDecls.append("all\"");
                } else {
                    filterStateDecls.append("\"");
                }
                filterStateDecls.append(");\n\n");

                filterQueryKeysInit
                        .append("    ")
                        .append(name)
                        .append(": filter")
                        .append(GeneratorTypeResolver.capitalize(name))
                        .append(",\n");
                filterQueryKeysSearch
                        .append("      ")
                        .append(name)
                        .append(": filter")
                        .append(GeneratorTypeResolver.capitalize(name))
                        .append(",\n");
                filterReset
                        .append("    setFilter")
                        .append(GeneratorTypeResolver.capitalize(name))
                        .append("(\"");
                if (field.type() == FieldType.BOOLEAN) filterReset.append("all");
                filterReset.append("\");\n");

                if (field.type() == FieldType.BOOLEAN) {
                    filterApiArgs
                            .append("        ")
                            .append(name)
                            .append(": filter")
                            .append(GeneratorTypeResolver.capitalize(name))
                            .append(" === \"all\" ? undefined : filter")
                            .append(GeneratorTypeResolver.capitalize(name))
                            .append(" === \"yes\",\n");
                    filterFields
                            .append("        <QueryField width={144}>\n")
                            .append("          <Select\n")
                            .append("            label={t(\"common.status\")}\n")
                            .append("            value={filter")
                            .append(GeneratorTypeResolver.capitalize(name))
                            .append("}\n")
                            .append("            onChange={(e) => setFilter")
                            .append(GeneratorTypeResolver.capitalize(name))
                            .append("(e.target.value)}\n")
                            .append("            options={[\n")
                            .append("              { value: \"all\", label: t(\"common.all\") },\n")
                            .append("              { value: \"yes\", label: t(\"common.enabled\") },\n")
                            .append("              { value: \"no\", label: t(\"common.disabled\") },\n")
                            .append("            ]}\n")
                            .append("          />\n")
                            .append("        </QueryField>\n");
                } else {
                    filterApiArgs
                            .append("        ")
                            .append(name)
                            .append(": filter")
                            .append(GeneratorTypeResolver.capitalize(name))
                            .append(" || undefined,\n");
                    String cap = GeneratorTypeResolver.capitalize(name);
                    String tfSingle = "        <TextField label=\"" + name + "\" value={filter" + cap
                            + "} onChange={(e) => setFilter" + cap + "(e.target.value)} />";
                    if (tfSingle.length() <= 100) {
                        filterFields.append(tfSingle).append("\n");
                    } else {
                        filterFields
                                .append("        <TextField\n")
                                .append("          label=\"")
                                .append(name)
                                .append("\"\n")
                                .append("          value={filter")
                                .append(cap)
                                .append("}\n")
                                .append("          onChange={(e) => setFilter")
                                .append(cap)
                                .append("(e.target.value)}\n")
                                .append("        />\n");
                    }
                }
            }
        }
        String initPrefix = "  const [form, setForm] = useState<" + entity + "Payload>(";
        String emptyFormInit = renderFormObject(emptyPairs, initPrefix, 2);
        String emptyFormCreate = renderFormObject(emptyPairs, "    setForm(", 4);
        String editFormMap = renderFormObject(editPairs, "    setForm(", 4);
        return templates
                .loadTemplate("frontend-page.tsx.tpl")
                .replace("{{E}}", entity)
                .replace("{{EL}}", entityLower)
                .replace("{{MODULE}}", module)
                .replace("{{RESOURCE}}", resource)
                .replace("{{PERM}}", perm)
                .replace("{{FILTER_STATE_DECLS}}", filterStateDecls.toString())
                .replace("{{FILTER_QUERY_KEYS_INIT}}", filterQueryKeysInit.toString())
                .replace("{{FILTER_QUERY_KEYS_SEARCH}}", filterQueryKeysSearch.toString())
                .replace("{{FILTER_API_ARGS}}", filterApiArgs.toString())
                .replace("{{FILTER_RESET}}", filterReset.toString())
                .replace("{{FILTER_FIELDS}}", filterFields.toString())
                .replace("{{COLUMNS}}", columns.toString())
                .replace("{{FORM_FIELDS}}", formFields.toString())
                .replace("{{EMPTY_FORM_INIT}}", emptyFormInit)
                .replace("{{EMPTY_FORM_CREATE}}", emptyFormCreate)
                .replace("{{EDIT_FORM_MAP}}", editFormMap)
                .replace("\r\n", "\n");
    }

    // ---------- helpers ----------

    private String renderFormObject(List<String> pairs, String prefix, int baseIndent) {
        String content = String.join(", ", pairs);
        String singleLine = "{ " + content + " }";
        int totalLength = prefix.length() + singleLine.length() + 2;
        if (totalLength <= 100) {
            return singleLine;
        }
        String fieldIndent = " ".repeat(baseIndent + 2);
        String closeIndent = " ".repeat(baseIndent);
        StringBuilder multi = new StringBuilder("{\n");
        for (String pair : pairs) {
            multi.append(fieldIndent).append(pair).append(",\n");
        }
        multi.append(closeIndent).append("}");
        return multi.toString();
    }

    private String renderFormOnChange(String name, String valueExpr) {
        String single = "          onChange={(e) => setForm((f) => ({ ...f, " + name + ": " + valueExpr + " }))}";
        if (single.length() <= 100) {
            return single + "\n";
        }
        return "          onChange={(e) =>\n"
                + "            setForm((f) => ({ ...f, " + name + ": " + valueExpr + " }))\n"
                + "          }\n";
    }
}
