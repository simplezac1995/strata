// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Chip, Grid, Stack, Typography } from "@mui/material";
import StorageOutlinedIcon from "@mui/icons-material/StorageOutlined";
import CreditCardOutlinedIcon from "@mui/icons-material/CreditCardOutlined";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import type { SvgIconComponent } from "@mui/icons-material";
import { useI18n } from "@/shared/i18n";
import { Button, PageHeader } from "@/components/ui/index";
import { pluginApi } from "@/shared/api/demo";
import { toast } from "@/shared/pageKit/Toast";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

export function PluginsOverview() {
  const { t } = useI18n();
  const [status, setStatus] = useState<{ storageLocal: boolean; paymentStripe: boolean } | null>(
    null,
  );

  useEffect(() => {
    pluginApi
      .status()
      .then(setStatus)
      .catch(() => toast.error("toast.loadFailed"));
  }, []);

  const cards: {
    id: string;
    Icon: SvgIconComponent;
    nameKey: string;
    descKey: string;
    enabled: boolean;
    version: string;
    to: string;
  }[] = [
    {
      id: "storage",
      Icon: StorageOutlinedIcon,
      nameKey: "plugins.pluginsOverview.storage",
      descKey: "plugins.card.storage.desc",
      enabled: status?.storageLocal ?? false,
      version: "v1.0.0",
      to: "/plugins/storage",
    },
    {
      id: "payments",
      Icon: CreditCardOutlinedIcon,
      nameKey: "plugins.card.payments.name",
      descKey: "plugins.card.payments.desc",
      enabled: status?.paymentStripe ?? false,
      version: "v1.0.0",
      to: "/plugins/payments",
    },
    {
      id: "ai",
      Icon: SmartToyOutlinedIcon,
      nameKey: "system.common.aiPack",
      descKey: "plugins.card.ai.desc",
      enabled: true,
      version: "v1.0.0",
      to: "/ai",
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("plugins.pluginsOverview.plugins")}
        subtitle={t("plugins.pluginsOverview.installedIntegrationModules")}
      />
      <Grid container spacing={2.5}>
        {cards.map((p) => (
          <Grid key={p.id} size={{ xs: 12, md: 4 }}>
            <ContentCard
              sx={{ height: "100%", display: "flex", flexDirection: "column", gap: 1.5 }}
            >
              <Stack
                direction="row"
                sx={{ justifyContent: "space-between", alignItems: "flex-start" }}
              >
                <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                  <Box
                    sx={{
                      width: 40,
                      height: 40,
                      borderRadius: 3,
                      bgcolor: (theme) =>
                        theme.palette.mode === "light"
                          ? "rgba(79, 70, 229, 0.1)"
                          : "rgba(79, 70, 229, 0.2)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      color: "primary.main",
                      flexShrink: 0,
                    }}
                  >
                    <p.Icon sx={{ fontSize: 22 }} />
                  </Box>
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                      {t(p.nameKey)}
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.disabled"
                      sx={{ fontFamily: "monospace" }}
                    >
                      {p.version}
                    </Typography>
                  </Box>
                </Stack>
                <Chip
                  size="small"
                  label={
                    status === null || status === undefined
                      ? "—"
                      : p.enabled
                        ? t("plugins.pluginsOverview.active2")
                        : t("plugins.pluginsOverview.inactive")
                  }
                  color={p.enabled ? "success" : "default"}
                  variant="outlined"
                />
              </Stack>
              <Typography variant="body2" color="text.secondary">
                {t(p.descKey)}
              </Typography>
              <Stack direction="row" spacing={1} sx={{ mt: "auto", pt: 1 }}>
                <Button
                  variant="outlined"
                  color="default"
                  size="sm"
                  onClick={() => (window.location.href = p.to)}
                >
                  {t("plugins.pluginsOverview.open")}
                </Button>
              </Stack>
            </ContentCard>
          </Grid>
        ))}
      </Grid>
    </PageStack>
  );
}

export default PluginsOverview;
