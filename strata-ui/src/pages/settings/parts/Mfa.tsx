// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import QrCode2Icon from "@mui/icons-material/QrCode2";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import { Box, Divider, Stack, Typography } from "@mui/material";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Chip, Dialog, TextField } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { authApi, type TotpEnrollResponse } from "@/shared/api/auth";
import { useAuthStore } from "@/shared/auth/store";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { PageSection as SettingsSection } from "@/shared/pageKit/PageWorkspace";

const CODE_PATTERN = /^\d{6}$/;
const MFA_STATUS_KEY = ["api", "system", "user", "mfa-status"] as const;

export function Mfa() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);

  const [enrollData, setEnrollData] = useState<TotpEnrollResponse | null>(null);
  const [code, setCode] = useState("");
  const [codeError, setCodeError] = useState("");
  const [disableOpen, setDisableOpen] = useState(false);
  const [disableCode, setDisableCode] = useState("");
  const [disableError, setDisableError] = useState("");

  const { data: enabled = false, isLoading } = useQuery({
    queryKey: [...MFA_STATUS_KEY, user?.id ?? "me"],
    queryFn: () => Boolean(useAuthStore.getState().user?.mfaEnabled),
    enabled: Boolean(user?.id),
    initialData: () => Boolean(useAuthStore.getState().user?.mfaEnabled),
  });

  const enrollMutation = useMutationWithToast(
    () => authApi.mfaEnroll(),
    { success: "settings.mfa.toast.enrolled", failure: "settings.mfa.toast.enrollFailed" },
    {
      onSuccess: (data) => {
        setEnrollData(data);
        setCode("");
        setCodeError("");
      },
    },
  );

  const confirmMutation = useMutationWithToast(
    (value: string) => authApi.mfaConfirm(value),
    { success: "settings.mfa.toast.confirmed", failure: "settings.mfa.toast.confirmFailed" },
    {
      onSuccess: () => {
        setEnrollData(null);
        setCode("");
        queryClient.setQueryData([...MFA_STATUS_KEY, user?.id ?? "me"], true);
        // Persist the flipped MFA flag into the auth store (and localStorage)
        // so other surfaces reading `user.mfaEnabled` (e.g. rehydrate, lock
        // screen) see the new value without a full reload.
        if (user) {
          useAuthStore.getState().setUser({ ...user, mfaEnabled: true });
        }
      },
    },
  );

  const disableMutation = useMutationWithToast(
    (value: string) => authApi.mfaDisable(value),
    { success: "settings.mfa.toast.disabled", failure: "settings.mfa.toast.disableFailed" },
    {
      onSuccess: () => {
        setDisableOpen(false);
        setDisableCode("");
        setDisableError("");
        queryClient.setQueryData([...MFA_STATUS_KEY, user?.id ?? "me"], false);
        // Mirror the disable into the persisted auth user object.
        if (user) {
          useAuthStore.getState().setUser({ ...user, mfaEnabled: false });
        }
      },
    },
  );

  function handleEnroll() {
    setCodeError("");
    void enrollMutation.mutateAsync().catch(() => {});
  }

  function handleConfirm() {
    if (!CODE_PATTERN.test(code.trim())) {
      setCodeError(t("settings.mfa.codeInvalid"));
      return;
    }
    setCodeError("");
    void confirmMutation.mutateAsync(code.trim()).catch(() => {});
  }

  function handleDisableConfirm() {
    if (!CODE_PATTERN.test(disableCode.trim())) {
      setDisableError(t("settings.mfa.codeInvalid"));
      return;
    }
    setDisableError("");
    void disableMutation.mutateAsync(disableCode.trim()).catch(() => {});
  }

  const enrollBusy = enrollMutation.isPending;
  const confirmBusy = confirmMutation.isPending;
  const disableBusy = disableMutation.isPending;

  return (
    <SettingsSection
      title={
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <ShieldOutlinedIcon sx={{ color: enabled ? "success.main" : "text.secondary" }} />
          <span>{t("settings.mfa.title")}</span>
          <Chip
            label={enabled ? t("common.enabled") : t("common.disabled")}
            color={enabled ? "success" : "default"}
          />
        </Stack>
      }
      description={t("settings.mfa.desc")}
      action={
        enabled ? (
          <Button
            variant="outlined"
            color="danger"
            size="sm"
            onClick={() => {
              setDisableOpen(true);
              setDisableCode("");
              setDisableError("");
            }}
          >
            {t("settings.mfa.disable")}
          </Button>
        ) : (
          <Button
            variant="contained"
            size="sm"
            disabled={isLoading || enrollBusy || Boolean(enrollData)}
            onClick={handleEnroll}
          >
            {enrollBusy ? t("common.loading") : t("settings.mfa.enable")}
          </Button>
        )
      }
    >
      {enabled ? (
        <Typography variant="body2" color="text.secondary">
          {t("settings.mfa.enabledHint")}
        </Typography>
      ) : enrollData ? (
        <Stack spacing={2.5}>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={3}
            sx={{ alignItems: "flex-start" }}
          >
            <Box
              sx={{
                width: 176,
                height: 176,
                borderRadius: 2,
                border: 1,
                borderColor: "divider",
                bgcolor: "background.paper",
                display: "grid",
                placeItems: "center",
                overflow: "hidden",
                flexShrink: 0,
              }}
            >
              {enrollData.qrCodeDataUrl ? (
                <img
                  src={enrollData.qrCodeDataUrl}
                  alt={t("settings.mfa.qrAlt")}
                  style={{ width: 160, height: 160, objectFit: "contain" }}
                />
              ) : (
                <QrCode2Icon sx={{ fontSize: 80, color: "text.disabled" }} />
              )}
            </Box>
            <Box sx={{ minWidth: 0, flex: 1 }}>
              <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                {t("settings.mfa.scanHint")}
              </Typography>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ mt: 1.5, display: "block", fontWeight: 600 }}
              >
                {t("settings.mfa.manualKey")}
              </Typography>
              <Typography
                variant="body2"
                sx={{
                  mt: 0.5,
                  p: 1.5,
                  borderRadius: 1.5,
                  bgcolor: "action.hover",
                  fontFamily: "monospace",
                  wordBreak: "break-all",
                }}
              >
                {enrollData.secret}
              </Typography>
            </Box>
          </Stack>

          <Divider />

          <Stack spacing={2.5} sx={{ maxWidth: 420 }}>
            <TextField
              label={t("settings.mfa.codeLabel")}
              placeholder={t("settings.mfa.codePlaceholder")}
              value={code}
              error={codeError}
              slotProps={{ htmlInput: { maxLength: 6, inputMode: "numeric" } }}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
            />
            <Stack direction="row" spacing={1.5}>
              <Button
                variant="contained"
                disabled={confirmBusy || code.length !== 6}
                onClick={handleConfirm}
              >
                {confirmBusy ? t("common.saving") : t("settings.mfa.confirm")}
              </Button>
              <Button
                variant="outlined"
                color="default"
                onClick={() => {
                  setEnrollData(null);
                  setCode("");
                  setCodeError("");
                }}
              >
                {t("common.cancel")}
              </Button>
            </Stack>
          </Stack>
        </Stack>
      ) : (
        <Typography variant="body2" color="text.secondary">
          {t("settings.mfa.disabledHint")}
        </Typography>
      )}

      <Dialog
        open={disableOpen}
        onClose={() => setDisableOpen(false)}
        title={t("settings.mfa.disableDialogTitle")}
      >
        <Stack spacing={2.5} sx={{ pt: 1, maxWidth: 420 }}>
          <Typography variant="body2" color="text.secondary">
            {t("settings.mfa.disableDialogDesc")}
          </Typography>
          <TextField
            label={t("settings.mfa.codeLabel")}
            placeholder={t("settings.mfa.codePlaceholder")}
            value={disableCode}
            error={disableError}
            slotProps={{ htmlInput: { maxLength: 6, inputMode: "numeric" } }}
            onChange={(e) => setDisableCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
          />
          <Stack direction="row" spacing={1.5} sx={{ justifyContent: "flex-end" }}>
            <Button variant="outlined" color="default" onClick={() => setDisableOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              variant="contained"
              color="danger"
              disabled={disableBusy || disableCode.length !== 6}
              onClick={handleDisableConfirm}
            >
              {disableBusy ? t("common.saving") : t("settings.mfa.disable")}
            </Button>
          </Stack>
        </Stack>
      </Dialog>
    </SettingsSection>
  );
}

export default Mfa;
