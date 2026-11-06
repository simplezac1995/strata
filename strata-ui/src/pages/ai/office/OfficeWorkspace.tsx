// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, CircularProgress, Grid, Stack, Typography } from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import { Button, TextArea, Tabs } from "@/components/ui/index";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { aiOfficeApi, type AiOfficeAction, type AiUsage } from "@/shared/api/aiOffice";
import { failureMessage } from "@/shared/hooks/useCrud";
import { toast } from "@/shared/pageKit/Toast";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";

const TABS: { id: AiOfficeAction; labelKey: string }[] = [
  { id: "draft", labelKey: "ai.office.tabDraft" },
  { id: "rewrite", labelKey: "ai.office.tabRewrite" },
  { id: "translate", labelKey: "ai.office.tabTranslate" },
  { id: "expand", labelKey: "ai.office.tabExpand" },
  { id: "report", labelKey: "ai.office.tabReport" },
];

export function OfficeWorkspace() {
  const { t } = useI18n();
  const [active, setActive] = useState<AiOfficeAction>("draft");
  const [input, setInput] = useState("");
  const [output, setOutput] = useState("");
  const [usage, setUsage] = useState<AiUsage | null>(null);
  const [busy, setBusy] = useState(false);

  async function run() {
    const text = input.trim();
    if (!text || busy) return;
    setBusy(true);
    setOutput("");
    setUsage(null);
    try {
      const res = await aiOfficeApi.run(active, { input: text });
      setOutput(res.result ?? "");
      setUsage(res.usage ?? null);
    } catch (err) {
      toast.error(failureMessage(err, "toast.saveFailed"));
    } finally {
      setBusy(false);
    }
  }

  async function copyOutput() {
    if (!output) return;
    try {
      await navigator.clipboard.writeText(output);
      toast.success("common.copied");
    } catch {
      toast.error("toast.saveFailed");
    }
  }

  const isPrompt = active === "draft" || active === "report";
  const inputLabel = isPrompt ? t("ai.office.promptLabel") : t("ai.office.inputLabel");

  return (
    <PageStack>
      <AiPageHeader title={t("ai.office.title")} subtitle={t("ai.office.subtitle")} />
      <Tabs
        tabs={TABS.map((tab) => ({ id: tab.id, label: t(tab.labelKey) }))}
        active={active}
        onChange={(id) => {
          setActive(id as AiOfficeAction);
          setOutput("");
          setUsage(null);
        }}
      />

      <Grid container spacing={2.5} sx={{ height: 520 }}>
        <Grid size={{ xs: 12, md: 6 }} sx={{ display: "flex", flexDirection: "column" }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              {inputLabel}
            </Typography>
            <Box sx={{ ml: "auto" }}>
              <Button
                variant="contained"
                size="sm"
                disabled={!input.trim() || busy}
                onClick={() => void run()}
              >
                {busy ? t("ai.common.generating") : t("ai.office.run")}
              </Button>
            </Box>
          </Stack>
          <Box sx={{ flex: 1, minHeight: 0 }}>
            <TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={busy}
              placeholder={t("ai.office.inputPlaceholder")}
              sx={{ height: "100%", "& textarea": { height: "100% !important" } }}
            />
          </Box>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }} sx={{ display: "flex", flexDirection: "column" }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              {t("ai.office.outputLabel")}
            </Typography>
            <Box sx={{ ml: "auto" }}>
              <Button
                variant="outlined"
                size="sm"
                disabled={!output || busy}
                onClick={() => void copyOutput()}
              >
                <ContentCopyIcon fontSize="small" sx={{ mr: 0.5 }} />
                {t("common.copy")}
              </Button>
            </Box>
          </Stack>
          <Box sx={{ flex: 1, minHeight: 0 }}>
            {busy ? (
              <ContentCard
                padding={1.5}
                sx={{
                  height: "100%",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <Stack spacing={1.5} sx={{ alignItems: "center" }}>
                  <CircularProgress size={28} />
                  <Typography variant="caption" color="text.secondary">
                    {t("ai.common.generating")}
                  </Typography>
                </Stack>
              </ContentCard>
            ) : output ? (
              <ContentCard padding={1.5} sx={{ height: "100%", overflowY: "auto" }}>
                <Typography
                  component="pre"
                  variant="body2"
                  sx={{ whiteSpace: "pre-wrap", fontFamily: "inherit", m: 0 }}
                >
                  {output}
                </Typography>
                {usage && (
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{
                      display: "block",
                      mt: 1.5,
                      pt: 1.5,
                      borderTop: 1,
                      borderColor: "divider",
                    }}
                  >
                    {t("ai.office.usageLabel")}: {t("ai.office.usagePrompt")} {usage.promptTokens} ·{" "}
                    {t("ai.office.usageCompletion")} {usage.completionTokens} ·{" "}
                    {t("ai.office.usageTotal")} {usage.totalTokens}
                  </Typography>
                )}
              </ContentCard>
            ) : (
              <ContentCard padding={1.5} sx={{ height: "100%" }}>
                <Typography variant="caption" color="text.secondary">
                  {t("ai.office.outputEmpty")}
                </Typography>
              </ContentCard>
            )}
          </Box>
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default OfficeWorkspace;
