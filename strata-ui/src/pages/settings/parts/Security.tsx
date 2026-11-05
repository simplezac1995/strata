// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import SecurityOutlinedIcon from "@mui/icons-material/SecurityOutlined";
import HistoryOutlinedIcon from "@mui/icons-material/HistoryOutlined";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Grid,
  Stack,
  Typography,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { Button, DataTable, Pagination, TextField } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { authApi } from "@/shared/api/auth";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { profileApi } from "@/shared/api/profile";
import type { OperationLogDto } from "@/shared/types";
import {
  PageHero as SettingsHero,
  PageSection as SettingsSection,
  PageWorkspace as SettingsWorkspace,
} from "@/shared/pageKit/PageWorkspace";
import { Mfa } from "./Mfa";

export function Security() {
  const { t } = useI18n();
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");

  const changePasswordMutation = useMutationWithToast(
    () => authApi.changePassword(oldPassword, newPassword),
    { success: "toast.saved", failure: "toast.saveFailed" },
    {
      onSuccess: () => {
        setOldPassword("");
        setNewPassword("");
        setConfirmPassword("");
      },
    },
  );

  const busy = changePasswordMutation.isPending;

  async function handleChangePassword() {
    if (!oldPassword) {
      setError(t("settings.security.currentPasswordRequired"));
      return;
    }
    if (newPassword.length < 6) {
      setError(t("error.new.password.must.be.at.least.6.characters"));
      return;
    }
    if (newPassword !== confirmPassword) {
      setError(t("error.passwords.do.not.match"));
      return;
    }
    setError("");
    try {
      await changePasswordMutation.mutateAsync();
    } catch {
      // toast already shown
    }
  }

  const logs = usePagedQuery<OperationLogDto>(
    ["api", "me", "operation-logs"],
    (pageIndex, size) => profileApi.operationLogs(pageIndex, size),
    { initialPageSize: 10 },
  );

  const logCols = [
    {
      key: "moduleName",
      title: t("settings.security.logs.module"),
      render: (r: OperationLogDto) => r.moduleName,
    },
    {
      key: "actionName",
      title: t("settings.security.logs.action"),
      render: (r: OperationLogDto) => r.actionName,
    },
    {
      key: "operatorName",
      title: t("settings.security.logs.operator"),
      render: (r: OperationLogDto) => r.operatorName || t("common.emDash"),
    },
    {
      key: "createdAt",
      title: t("settings.security.logs.time"),
      width: 180,
      render: (r: OperationLogDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {r.createdAt ? new Date(r.createdAt).toLocaleString() : "—"}
        </Typography>
      ),
    },
  ];

  return (
    <SettingsWorkspace>
      <SettingsHero title={t("nav.security")} subtitle={t("settings.security.subtitle")} />

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 5 }}>
          <SettingsSection
            title={t("settings.security.tipsTitle")}
            description={t("settings.security.tipsDesc")}
          >
            <Stack spacing={2}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start" }}>
                <SecurityOutlinedIcon sx={{ color: "primary.main", mt: 0.25 }} />
                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                  {t("settings.security.tipUnique")}
                </Typography>
              </Stack>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start" }}>
                <LockOutlinedIcon sx={{ color: "primary.main", mt: 0.25 }} />
                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                  {t("settings.security.tipRotate")}
                </Typography>
              </Stack>
            </Stack>
          </SettingsSection>
        </Grid>

        <Grid size={{ xs: 12, lg: 7 }}>
          <SettingsSection title={t("settings.security.changePassword")}>
            <Stack spacing={2.5} sx={{ maxWidth: 480 }}>
              <TextField
                label={t("settings.security.field.currentPassword")}
                type="password"
                placeholder={"\u2022".repeat(8)}
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
              />
              <TextField
                label={t("settings.security.field.newPassword")}
                type="password"
                placeholder={"\u2022".repeat(8)}
                hint={t("system.users.passwordPlaceholder")}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
              <TextField
                label={t("settings.security.field.confirmPassword")}
                type="password"
                placeholder={"\u2022".repeat(8)}
                error={error}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
              <Button
                variant="contained"
                disabled={busy}
                onClick={() => void handleChangePassword()}
              >
                {busy ? t("common.saving") : t("settings.security.updatePassword")}
              </Button>
            </Stack>
          </SettingsSection>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Mfa />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Accordion>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                <HistoryOutlinedIcon color="primary" />
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {t("settings.security.operationLogsTitle")}
                </Typography>
              </Stack>
            </AccordionSummary>
            <AccordionDetails>
              <Stack spacing={2}>
                <DataTable
                  columns={logCols}
                  data={logs.rows}
                  loading={logs.loading}
                  rowKey={(r) => r.id}
                  tableId="me.operation-logs"
                  error={logs.error}
                  onRetry={logs.reload}
                />
                <Pagination
                  page={logs.page}
                  pageSize={logs.pageSize}
                  total={logs.total}
                  onPage={logs.setPage}
                  onPageSize={logs.setPageSize}
                />
              </Stack>
            </AccordionDetails>
          </Accordion>
        </Grid>
      </Grid>
    </SettingsWorkspace>
  );
}

export default Security;
