// SPDX-License-Identifier: Apache-2.0
import { Box, Grid, Stack, Tab, Tabs, Typography } from "@mui/material";
import { Button, Select, PageHeader, TextField, DataTable } from "@/components/ui/index";
import { IfPerm } from "@/shared/pageKit/IfPerm";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";
import { PRESETS, blankField } from "./codegenerator/part-1";
import { useCodeGenerator } from "./codegenerator/useCodeGenerator";

export function CodeGenerator() {
  const {
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
  } = useCodeGenerator();

  return (
    <PageStack>
      <PageHeader title={t("nav.code.generator")} subtitle={t("tools.generator.subtitle")} />

      <ContentCard>
        <Stack spacing={2}>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6, lg: 4 }}>
              <Select
                label={t("tools.generator.presetTable")}
                value={form.tableName}
                onChange={(e) => applyPreset(e.target.value)}
                options={PRESETS.map((p) => ({ value: p.tableName, label: p.tableName }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4 }}>
              <TextField
                label={t("common.module")}
                value={form.module}
                onChange={(e) => setForm((prev) => ({ ...prev, module: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4 }}>
              <TextField
                label={t("tools.generator.tableName")}
                value={form.tableName}
                onChange={(e) => setForm((prev) => ({ ...prev, tableName: e.target.value }))}
                placeholder={t("tools.generator.placeholder.tableName")}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4 }}>
              <TextField
                label={t("tools.generator.entityName")}
                value={form.entityName}
                onChange={(e) => setForm((prev) => ({ ...prev, entityName: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4 }}>
              <TextField
                label={t("tools.generator.resource")}
                value={form.resource}
                onChange={(e) => setForm((prev) => ({ ...prev, resource: e.target.value }))}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4 }}>
              <TextField
                label={t("tools.generator.menuLabel")}
                value={form.menuLabel}
                onChange={(e) => setForm((prev) => ({ ...prev, menuLabel: e.target.value }))}
              />
            </Grid>
          </Grid>

          <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap" }}>
            <Button variant="contained" onClick={() => void handleGenerate()} disabled={generating}>
              {t("tools.generator.generate")}
            </Button>
            <Button
              variant="outlined"
              color="default"
              onClick={() => void handleDownload()}
              disabled={files.length === 0}
            >
              {t("tools.generator.downloadZip")}
            </Button>
            <IfPerm anyOf={["generator:create"]}>
              <Button
                variant="outlined"
                color="default"
                onClick={() => void handleWrite()}
                disabled={files.length === 0}
              >
                {t("tools.generator.writeToDisk")}
              </Button>
            </IfPerm>
            <Button
              variant="outlined"
              color="default"
              onClick={() =>
                setForm((prev) => ({ ...prev, fields: [...prev.fields, blankField()] }))
              }
            >
              {t("tools.generator.addField")}
            </Button>
          </Stack>
        </Stack>
      </ContentCard>

      <ContentCard>
        <DataTable
          columns={fieldCols}
          data={form.fields}
          rowKey={(row) => `${row.name}-${form.fields.indexOf(row)}`}
        />
      </ContentCard>

      {files.length > 0 && (
        <ContentCard padding={false}>
          {writtenPaths !== null && (
            <Box sx={{ px: 2.5, pt: 2 }}>
              <Typography variant="subtitle2" color="success.main" gutterBottom>
                {t("tools.generator.writtenToDisk")}
              </Typography>
              <Box component="ul" sx={{ m: 0, pl: 3 }}>
                {writtenPaths.map((path) => (
                  <Typography
                    key={path}
                    component="li"
                    variant="body2"
                    sx={{ fontFamily: "monospace" }}
                  >
                    {path}
                  </Typography>
                ))}
              </Box>
            </Box>
          )}
          <Tabs
            value={activeFile}
            onChange={(_, v) => setActiveFile(v)}
            variant="scrollable"
            scrollButtons="auto"
            sx={{ borderBottom: 1, borderColor: "divider" }}
          >
            {files.map((file) => (
              <Tab
                key={file.path}
                value={file.path}
                label={file.path}
                sx={{ textTransform: "none", fontSize: 13 }}
              />
            ))}
          </Tabs>
          <Box
            component="pre"
            sx={{
              p: 2.5,
              m: 0,
              fontSize: 12,
              fontFamily: "monospace",
              overflow: "auto",
              maxHeight: 480,
              bgcolor: "action.hover",
            }}
          >
            <Box component="code">{activeContent}</Box>
          </Box>
        </ContentCard>
      )}
    </PageStack>
  );
}

export default CodeGenerator;
