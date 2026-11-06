// SPDX-License-Identifier: Apache-2.0
import { Stack, Typography, Box } from "@mui/material";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import { useI18n } from "@/shared/i18n";
import { Button, PageHeader, Badge } from "@/components/ui/index";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

export function AiPluginEmpty() {
  const { t } = useI18n();
  return (
    <PageStack>
      <PageHeader
        title={t("system.common.aiPack")}
        subtitle={t("plugins.aiPluginEmpty.unlockAiPoweredFeatures")}
      />
      <ContentCard sx={{ py: 8, textAlign: "center", maxWidth: 512, mx: "auto" }}>
        <Box
          sx={{
            width: 56,
            height: 56,
            borderRadius: 3,
            bgcolor: (theme) =>
              theme.palette.mode === "light" ? "rgba(79, 70, 229, 0.1)" : "rgba(79, 70, 229, 0.2)",
            color: "primary.main",
            display: "inline-flex",
            alignItems: "center",
            justifyContent: "center",
            mb: 2,
          }}
        >
          <SmartToyOutlinedIcon sx={{ fontSize: 28 }} />
        </Box>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
          {t("plugins.aiPluginEmpty.aiPackNotEnabled")}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {t("settings.site.enableAiPackHint")}
        </Typography>
        <Badge variant="ai">{t("ui.badge.aiPack")}</Badge>
        <Stack direction="row" spacing={1.5} sx={{ mt: 2.5, justifyContent: "center" }}>
          <Button
            variant="contained"
            onClick={() => (window.location.href = "/system/site-settings")}
          >
            {t("settings.site.open")}
          </Button>
        </Stack>
      </ContentCard>
    </PageStack>
  );
}

export default AiPluginEmpty;
