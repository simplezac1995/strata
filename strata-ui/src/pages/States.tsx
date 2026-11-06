// SPDX-License-Identifier: Apache-2.0
import { useState, type ReactNode } from "react";
import { Box, Grid, Skeleton, Stack, Typography } from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import InboxOutlinedIcon from "@mui/icons-material/InboxOutlined";
import { Button as UiButton, Card, ConfirmDialog, PageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { PageStack } from "@/shared/pageKit/PageLayout";

function StateIcon({ children }: { children: ReactNode }) {
  return (
    <Box
      sx={{
        width: 56,
        height: 56,
        borderRadius: 3,
        mx: "auto",
        mb: 2,
        bgcolor: (theme) =>
          theme.palette.mode === "light" ? "rgba(79, 70, 229, 0.08)" : "rgba(255,255,255,0.06)",
        color: "text.secondary",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      {children}
    </Box>
  );
}

export function StatesBoard() {
  const { t } = useI18n();
  const [showDelete, setShowDelete] = useState(false);

  return (
    <PageStack>
      <PageHeader title={t("ui.states.board")} />
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <Card sx={{ textAlign: "center", py: 2 }}>
            <Typography sx={{ fontSize: 48, fontWeight: 600, color: "error.light", opacity: 0.5 }}>
              403
            </Typography>
            <StateIcon>
              <LockOutlinedIcon sx={{ fontSize: 24, color: "error.main" }} />
            </StateIcon>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 1 }}>
              {t("ui.states.forbidden")}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {t("ui.states.no.permission")}
            </Typography>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <Card sx={{ textAlign: "center", py: 2 }}>
            <Typography sx={{ fontSize: 48, fontWeight: 600, color: "action.selected" }}>
              404
            </Typography>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 1.5 }}>
              {t("ui.states.not.found")}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {t("ui.states.page.not.exist")}
            </Typography>
            <Stack direction="row" spacing={1} sx={{ justifyContent: "center", mt: 2 }}>
              <UiButton variant="contained" size="sm">
                {t("common.goHome")}
              </UiButton>
              <UiButton variant="outlined" color="default" size="sm">
                {t("common.goBack")}
              </UiButton>
            </Stack>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <Card sx={{ textAlign: "center", py: 2 }}>
            <StateIcon>
              <InboxOutlinedIcon sx={{ fontSize: 26 }} />
            </StateIcon>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {t("ui.states.no.data")}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {t("ui.states.no.records")}
            </Typography>
            <UiButton variant="outlined" color="default" size="sm" sx={{ mt: 2 }}>
              {t("ui.states.create.first")}
            </UiButton>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <Card>
            <Stack spacing={1.5}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                <Skeleton variant="circular" width={32} height={32} />
                <Box sx={{ flex: 1 }}>
                  <Skeleton height={12} />
                  <Skeleton height={10} width="66%" />
                </Box>
              </Stack>
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} variant="rounded" height={40} />
              ))}
              <Typography variant="caption" color="text.disabled">
                {t("ui.states.loading.skeleton")}
              </Typography>
            </Stack>
          </Card>
        </Grid>

        <Grid size={12}>
          <Card sx={{ textAlign: "center", py: 2 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
              {t("ui.states.delete.dialog")}
            </Typography>
            <UiButton variant="outlined" color="danger" onClick={() => setShowDelete(true)}>
              {t("ui.states.show.delete")}
            </UiButton>
            <ConfirmDialog
              open={showDelete}
              onClose={() => setShowDelete(false)}
              onConfirm={() => setShowDelete(false)}
              title={t("ui.states.delete.title")}
              description={t("ui.states.delete.desc")}
              confirmText={t("common.delete")}
              confirmColor="danger"
            />
          </Card>
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default StatesBoard;
