// SPDX-License-Identifier: Apache-2.0
// State and handlers of CodeGenerator, split out of CodeGenerator.tsx.

import { useMemo, useState } from "react";
import { Box, Typography } from "@mui/material";
import { useI18n } from "@/shared/i18n";
import { Button, Select, TextField, Checkbox } from "@/components/ui/index";
import {
  generatorApi,
  type FieldSpec,
  type FieldType,
  type GeneratorFile,
  type GeneratorRequest,
} from "@/shared/api/generator";
import { errorMessage } from "@/shared/hooks/useCrud";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { toast } from "@/shared/pageKit/Toast";
import {
  FIELD_TYPES,
  WidgetKind,
  WIDGET_OPTIONS,
  inferWidget,
  applyWidget,
  Preset,
  PRESETS,
  clonePreset,
} from "./part-1";

export function useCodeGenerator() {
  const { t } = useI18n();

  const [form, setForm] = useState<Preset>(() => clonePreset(PRESETS[0]));

  const [files, setFiles] = useState<GeneratorFile[]>([]);

  const [activeFile, setActiveFile] = useState("");

  const [writtenPaths, setWrittenPaths] = useState<string[] | null>(null);

  const [generating, setGenerating] = useState(false);

  const downloadMutation = useMutationWithToast(
    async () => {
      const blob = await generatorApi.download(request);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${form.entityName.toLowerCase()}-codegen.zip`;
      anchor.click();
      URL.revokeObjectURL(url);
    },
    { success: "toast.downloaded", failure: "toast.downloadFailed" },
  );

  const writeMutation = useMutationWithToast(
    async () => {
      const paths = await generatorApi.write(request);
      setWrittenPaths(paths);
    },
    { success: "tools.generator.writtenToDisk", failure: "toast.saveFailed" },
  );

  const request = useMemo<GeneratorRequest>(
    () => ({
      module: form.module,
      tableName: form.tableName,
      entityName: form.entityName,
      resource: form.resource,
      menuLabel: form.menuLabel,
      basePackage: "dev.strata.system",
      fields: form.fields,
    }),
    [form],
  );

  const activeContent = files.find((f) => f.path === activeFile)?.content ?? "";

  const applyPreset = (tableName: string) => {
    const preset = PRESETS.find((p) => p.tableName === tableName);
    if (!preset) return;
    setForm(clonePreset(preset));
    setFiles([]);
    setActiveFile("");
  };

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      const result = await generatorApi.preview(request);
      setFiles(result);
      setActiveFile(result[0]?.path ?? "");
    } catch (err) {
      toast.error(errorMessage(err, "toast.generateFailed"));
    } finally {
      setGenerating(false);
    }
  };

  const handleDownload = () => void downloadMutation.mutateAsync().catch(() => {});

  const handleWrite = () => void writeMutation.mutateAsync().catch(() => {});

  const updateField = (index: number, patch: Partial<FieldSpec>) => {
    setForm((prev) => ({
      ...prev,
      fields: prev.fields.map((field, i) => (i === index ? { ...field, ...patch } : field)),
    }));
  };

  const removeField = (index: number) => {
    setForm((prev) => ({ ...prev, fields: prev.fields.filter((_, i) => i !== index) }));
  };

  const fieldCols = [
    {
      key: "name",
      title: t("tools.generator.field.name"),
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        return (
          <Box sx={{ minWidth: 120 }}>
            <TextField
              value={row.name}
              onChange={(e) => updateField(rowIndex, { name: e.target.value })}
            />
          </Box>
        );
      },
    },
    {
      key: "type",
      title: t("common.typeRequired"),
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        return (
          <Box sx={{ minWidth: 110 }}>
            <Select
              value={row.type}
              onChange={(e) => updateField(rowIndex, { type: e.target.value as FieldType })}
              options={FIELD_TYPES.map((type) => ({ value: type, label: type }))}
            />
          </Box>
        );
      },
    },
    {
      key: "widget",
      title: t("tools.generator.field.widget"),
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        return (
          <Box sx={{ minWidth: 120 }}>
            <Select
              value={inferWidget(row)}
              onChange={(e) => updateField(rowIndex, applyWidget(e.target.value as WidgetKind))}
              options={WIDGET_OPTIONS}
            />
          </Box>
        );
      },
    },
    {
      key: "maxLength",
      title: t("tools.generator.field.maxLength"),
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        if (row.type !== "STRING") return <Typography color="text.disabled">—</Typography>;
        return (
          <Box sx={{ width: 80 }}>
            <TextField
              type="number"
              value={row.maxLength ?? ""}
              onChange={(e) => {
                const raw = e.target.value;
                updateField(rowIndex, {
                  maxLength: raw === "" ? null : Number.parseInt(raw, 10),
                });
              }}
            />
          </Box>
        );
      },
    },
    {
      key: "required",
      title: t("tools.generator.field.required"),
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        return (
          <Checkbox
            checked={row.required}
            onChange={(e) => updateField(rowIndex, { required: e.target.checked })}
          />
        );
      },
    },
    {
      key: "queryable",
      title: t("tools.generator.field.queryable"),
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        return (
          <Checkbox
            checked={row.queryable}
            onChange={(e) => updateField(rowIndex, { queryable: e.target.checked })}
          />
        );
      },
    },
    {
      key: "unique",
      title: "Unique",
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        return (
          <Checkbox
            checked={row.unique}
            onChange={(e) => updateField(rowIndex, { unique: e.target.checked })}
          />
        );
      },
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (row: FieldSpec) => {
        const rowIndex = form.fields.indexOf(row);
        return (
          <Button variant="text" size="sm" color="danger" onClick={() => removeField(rowIndex)}>
            {t("shell.favorite.remove")}
          </Button>
        );
      },
    },
  ];

  return {
    t,
    form,
    setForm,
    files,
    setFiles,
    activeFile,
    setActiveFile,
    writtenPaths,
    setWrittenPaths,
    generating,
    setGenerating,
    downloadMutation,
    writeMutation,
    request,
    activeContent,
    applyPreset,
    handleGenerate,
    handleDownload,
    handleWrite,
    updateField,
    removeField,
    fieldCols,
  };
}
