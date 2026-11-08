// SPDX-License-Identifier: Apache-2.0
import { useEffect, useMemo, useState } from "react";
import { Box, Divider, IconButton, MenuItem, Stack, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import { useI18n } from "@/shared/i18n";
import { Button } from "./Button";
import { Select, TextField } from "./Fields";

export type FilterOperator = "eq" | "neq" | "like" | "gt" | "lt" | "between";

export interface FilterFieldOption {
  value: string;
  label: string;
}

/** One combined AND condition: field <operator> value (or value..value2 for between). */
export interface AdvancedCondition {
  field: string;
  operator: FilterOperator;
  value: string;
  value2?: string;
}

interface FilterTemplate {
  name: string;
  conditions: AdvancedCondition[];
}

const TEMPLATES_PREFIX = "strata_filter_templates:v1:";

const OPERATORS: { value: FilterOperator; labelKey: string }[] = [
  { value: "eq", labelKey: "filter.op.eq" },
  { value: "neq", labelKey: "filter.op.neq" },
  { value: "like", labelKey: "filter.op.like" },
  { value: "gt", labelKey: "filter.op.gt" },
  { value: "lt", labelKey: "filter.op.lt" },
  { value: "between", labelKey: "filter.op.between" },
];

function emptyCondition(fields: FilterFieldOption[]): AdvancedCondition {
  return { field: fields[0]?.value ?? "", operator: "eq", value: "" };
}

function readTemplates(storageKey: string): FilterTemplate[] {
  try {
    const raw = localStorage.getItem(TEMPLATES_PREFIX + storageKey);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as FilterTemplate[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function writeTemplates(storageKey: string, templates: FilterTemplate[]) {
  try {
    localStorage.setItem(TEMPLATES_PREFIX + storageKey, JSON.stringify(templates));
  } catch {
    /* quota / private mode */
  }
}

export interface AdvancedFilterBuilderProps {
  /** Available fields the user can build conditions against. */
  fields: FilterFieldOption[];
  /** Stable key (e.g. table/page id) isolating saved templates in localStorage. */
  storageKey: string;
  /** Emits the current combined conditions on every change. */
  onChange: (conditions: AdvancedCondition[]) => void;
}

/**
 * Composable multi-condition filter builder. Renders rows of
 * `field + operator + value` combined with AND, and supports saving/loading
 * named templates to localStorage.
 *
 * Self-contained: it owns its own condition state and pushes changes upward
 * via `onChange`. It does not fetch data itself — the host page turns the
 * emitted conditions into query params.
 */
export function AdvancedFilterBuilder({
  fields,
  storageKey,
  onChange,
}: AdvancedFilterBuilderProps) {
  const { t } = useI18n();
  const [conditions, setConditions] = useState<AdvancedCondition[]>(() => [emptyCondition(fields)]);
  const [templates, setTemplates] = useState<FilterTemplate[]>(() => readTemplates(storageKey));
  const [templateName, setTemplateName] = useState("");
  const [selectedTemplate, setSelectedTemplate] = useState("");

  // Re-emit whenever conditions change so the host page stays in sync.
  useEffect(() => {
    onChange(conditions);
  }, [conditions, onChange]);

  // Keep the field default valid if the field list changes.
  useEffect(() => {
    setConditions((prev) =>
      prev.map((c) => ({
        ...c,
        field: fields.some((f) => f.value === c.field) ? c.field : (fields[0]?.value ?? ""),
      })),
    );
  }, [fields]);

  const persistTemplates = (next: FilterTemplate[]) => {
    setTemplates(next);
    writeTemplates(storageKey, next);
  };

  const updateCondition = (index: number, patch: Partial<AdvancedCondition>) => {
    setConditions((prev) => prev.map((c, i) => (i === index ? { ...c, ...patch } : c)));
  };

  const addCondition = () => {
    setConditions((prev) => [...prev, emptyCondition(fields)]);
  };

  const removeCondition = (index: number) => {
    setConditions((prev) => {
      const next = prev.filter((_, i) => i !== index);
      return next.length > 0 ? next : [emptyCondition(fields)];
    });
  };

  const saveTemplate = () => {
    const name = templateName.trim();
    if (!name) return;
    const active = conditions.filter((c) => c.value.trim() !== "");
    if (active.length === 0) return;
    const existing = templates.find((tp) => tp.name === name);
    const next = existing
      ? templates.map((tp) => (tp.name === name ? { name, conditions: active } : tp))
      : [...templates, { name, conditions: active }];
    persistTemplates(next);
    setTemplateName("");
  };

  const applyTemplate = (name: string) => {
    const tp = templates.find((x) => x.name === name);
    if (!tp) return;
    setConditions(
      tp.conditions.length > 0 ? tp.conditions.map((c) => ({ ...c })) : [emptyCondition(fields)],
    );
  };

  const deleteTemplate = (name: string) => {
    persistTemplates(templates.filter((tp) => tp.name !== name));
    if (selectedTemplate === name) setSelectedTemplate("");
  };

  const operatorOptions = useMemo(
    () => OPERATORS.map((o) => ({ value: o.value, label: t(o.labelKey) })),
    [t],
  );
  const fieldOptions = useMemo(
    () => fields.map((f) => ({ value: f.value, label: f.label })),
    [fields],
  );

  return (
    <Stack spacing={1.5}>
      {conditions.map((cond, idx) => (
        <Box key={idx} sx={{ display: "flex", alignItems: "flex-start", gap: 1, flexWrap: "wrap" }}>
          {idx > 0 ? (
            <Typography
              variant="caption"
              sx={{ pt: 1.6, width: 32, color: "text.secondary", fontWeight: 600 }}
            >
              {t("filter.and")}
            </Typography>
          ) : null}
          <Box sx={{ width: { xs: "100%", sm: 180 } }}>
            <Select
              size="small"
              value={cond.field}
              options={fieldOptions}
              onChange={(e) => updateCondition(idx, { field: e.target.value })}
            />
          </Box>
          <Box sx={{ width: { xs: "100%", sm: 130 } }}>
            <Select
              size="small"
              value={cond.operator}
              options={operatorOptions}
              onChange={(e) => updateCondition(idx, { operator: e.target.value as FilterOperator })}
            />
          </Box>
          {cond.operator === "between" ? (
            <>
              <Box sx={{ width: { xs: "100%", sm: 140 } }}>
                <TextField
                  size="small"
                  placeholder={t("filter.value")}
                  value={cond.value}
                  onChange={(e) => updateCondition(idx, { value: e.target.value })}
                />
              </Box>
              <Typography variant="caption" sx={{ pt: 1.6, color: "text.secondary" }}>
                ~
              </Typography>
              <Box sx={{ width: { xs: "100%", sm: 140 } }}>
                <TextField
                  size="small"
                  placeholder={t("filter.value2")}
                  value={cond.value2 ?? ""}
                  onChange={(e) => updateCondition(idx, { value2: e.target.value })}
                />
              </Box>
            </>
          ) : (
            <Box sx={{ width: { xs: "100%", sm: 200 } }}>
              <TextField
                size="small"
                placeholder={t("filter.value")}
                value={cond.value}
                onChange={(e) => updateCondition(idx, { value: e.target.value })}
              />
            </Box>
          )}
          <IconButton
            size="small"
            onClick={() => removeCondition(idx)}
            aria-label={t("filter.removeCondition")}
            sx={{ mt: 0.5 }}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      ))}

      <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
        <Button variant="outlined" size="sm" startIcon={<AddIcon />} onClick={addCondition}>
          {t("filter.addCondition")}
        </Button>
      </Box>

      <Divider />

      <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", alignItems: "flex-start" }}>
        <Box sx={{ width: { xs: "100%", sm: 200 } }}>
          <TextField
            size="small"
            placeholder={t("filter.templateName")}
            value={templateName}
            onChange={(e) => setTemplateName(e.target.value)}
          />
        </Box>
        <Button variant="outlined" size="sm" startIcon={<BookmarkIcon />} onClick={saveTemplate}>
          {t("filter.saveTemplate")}
        </Button>

        {templates.length > 0 ? (
          <>
            <Box sx={{ width: { xs: "100%", sm: 180 } }}>
              <Select
                size="small"
                value={selectedTemplate}
                displayEmpty
                onChange={(e) => {
                  const name = e.target.value;
                  setSelectedTemplate(name);
                  if (name) applyTemplate(name);
                }}
              >
                <MenuItem value="">{t("filter.templates")}</MenuItem>
                {templates.map((tp) => (
                  <MenuItem key={tp.name} value={tp.name}>
                    {tp.name}
                  </MenuItem>
                ))}
              </Select>
            </Box>
            {selectedTemplate ? (
              <Button
                variant="outlined"
                color="default"
                size="sm"
                onClick={() => deleteTemplate(selectedTemplate)}
              >
                {t("filter.deleteTemplate")}
              </Button>
            ) : null}
          </>
        ) : null}
      </Box>
    </Stack>
  );
}
