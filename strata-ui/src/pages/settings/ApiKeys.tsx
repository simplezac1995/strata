// SPDX-License-Identifier: Apache-2.0
import { useCallback, useEffect, useState } from "react";
import AddIcon from "@mui/icons-material/Add";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import KeyOutlinedIcon from "@mui/icons-material/KeyOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import CheckCircleOutlinedIcon from "@mui/icons-material/CheckCircleOutlined";
import CloseIcon from "@mui/icons-material/Close";
import {
  Alert,
  Box,
  Chip,
  DialogActions,
  Grid,
  IconButton,
  Stack,
  Typography,
} from "@mui/material";
import { Button, TextField, Dialog, Pagination } from "@/components/ui/index";
import { apiKeyApi } from "@/shared/api/system";
import { failureMessage } from "@/shared/hooks/useCrud";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { useAuthStore } from "@/shared/auth/store";
import { useI18n } from "@/shared/i18n";
import { toast } from "@/shared/pageKit/Toast";
import {
  PageHero as SettingsHero,
  PageMetric as SettingsMetric,
  PageMetricRow as SettingsMetricRow,
  PageSection as SettingsSection,
  PageWorkspace as SettingsWorkspace,
} from "@/shared/pageKit/PageWorkspace";
import type { ApiKeyDto } from "@/shared/types";

const ALL_SCOPES = ["read", "write", "admin", "deploy", "billing"];

const SCOPE_CHIP_COLOR: Record<
  string,
  "primary" | "secondary" | "error" | "warning" | "success" | "info" | "default"
> = {
  read: "info",
  write: "secondary",
  admin: "error",
  deploy: "warning",
  billing: "success",
};

function CreateModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (key: string) => void;
}) {
  const { t } = useI18n();
  const [name, setName] = useState("");
  const [scopes, setScopes] = useState<string[]>(["read"]);
  const [busy, setBusy] = useState(false);
  const toggle = (s: string) =>
    setScopes((p) => (p.includes(s) ? p.filter((x) => x !== s) : [...p, s]));

  const create = async () => {
    setBusy(true);
    try {
      const created = await apiKeyApi.create({
        name,
        scopes: scopes.join(","),
        enabled: true,
      });
      onCreated(created.plaintextKey);
      onClose();
      toast.success("toast.saved");
    } catch (err) {
      toast.error(failureMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} title={t("settings.apiKeys.create")} maxWidth="sm">
      <Stack spacing={2.5} sx={{ py: 1 }}>
        <TextField
          label={t("settings.apiKeys.field.name")}
          placeholder={t("settings.apiKeys.placeholder.name")}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <Box>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1.25 }}>
            {t("settings.apiKeys.permissions")}
          </Typography>
          <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap", gap: 1 }}>
            {ALL_SCOPES.map((s) => {
              const selected = scopes.includes(s);
              return (
                <Chip
                  key={s}
                  label={s}
                  clickable
                  onClick={() => toggle(s)}
                  color={selected ? "primary" : "default"}
                  variant={selected ? "filled" : "outlined"}
                />
              );
            })}
          </Stack>
        </Box>
      </Stack>
      <DialogActions sx={{ px: 0, pt: 2 }}>
        <Button variant="outlined" color="default" onClick={onClose}>
          {t("common.cancel")}
        </Button>
        <Button
          variant="contained"
          onClick={() => void create()}
          disabled={!name || scopes.length === 0 || busy}
        >
          {t("settings.apiKeys.generate")}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function NewKeyBanner({ value, onDismiss }: { value: string; onDismiss: () => void }) {
  const { t } = useI18n();
  const [copied, setCopied] = useState(false);
  const copy = () => {
    void navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };
  return (
    <Alert
      severity="success"
      icon={<CheckCircleOutlinedIcon />}
      action={
        <IconButton
          size="small"
          color="inherit"
          onClick={onDismiss}
          aria-label={t("common.dismiss")}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      }
    >
      <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 0.5 }}>
        {t("settings.apiKeys.generatedTitle")}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
        {t("settings.apiKeys.copyWarning")}
      </Typography>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={1.5}
        sx={{ alignItems: { sm: "center" } }}
      >
        <Box
          component="code"
          sx={{
            flex: 1,
            width: "100%",
            fontFamily: "monospace",
            fontSize: 13,
            bgcolor: "background.paper",
            border: 1,
            borderColor: "success.light",
            px: 2,
            py: 1.25,
            borderRadius: 2,
            wordBreak: "break-all",
          }}
        >
          {value}
        </Box>
        <Button variant="contained" color="success" onClick={copy} sx={{ flexShrink: 0 }}>
          {copied ? (
            <CheckCircleOutlinedIcon sx={{ fontSize: 18 }} />
          ) : (
            <ContentCopyIcon sx={{ fontSize: 18 }} />
          )}
          {copied ? t("shell.copied") : t("common.copy")}
        </Button>
      </Stack>
    </Alert>
  );
}

export function ApiKeys() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:apikey:create"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:apikey:delete"));
  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload } = usePagedQuery(
    ["api", "system", "api-keys"],
    apiKeyApi.page,
  );

  const deleteMutation = useMutationWithToast((id: string) => apiKeyApi.remove(id), {
    success: "toast.deleted",
    failure: "toast.deleteFailed",
  });
  const [activeCount, setActiveCount] = useState<number | null>(null);
  const [modal, setModal] = useState(false);
  const [newKey, setNewKey] = useState("");

  const reloadSummary = useCallback(async () => {
    try {
      const summary = await apiKeyApi.summary();
      setActiveCount(summary.active);
    } catch {
      setActiveCount(null);
    }
  }, []);

  useEffect(() => {
    void reloadSummary();
  }, [reloadSummary]);

  const scopesOf = (k: ApiKeyDto) =>
    (k.scopes ?? "")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);

  return (
    <SettingsWorkspace>
      <CreateModal
        open={modal}
        onClose={() => setModal(false)}
        onCreated={(k) => {
          setNewKey(k);
          void reload();
          void reloadSummary();
        }}
      />

      <SettingsHero
        title={t("nav.api.keys")}
        subtitle={t("settings.apiKeys.subtitle")}
        actions={
          canCreate ? (
            <Button variant="contained" onClick={() => setModal(true)}>
              <AddIcon sx={{ fontSize: 18 }} aria-hidden />
              {t("settings.apiKeys.newKey")}
            </Button>
          ) : undefined
        }
        footer={
          <SettingsMetricRow>
            <SettingsMetric
              label={t("settings.apiKeys.metricTotal")}
              value={loading ? "—" : total}
            />
            <SettingsMetric
              label={t("settings.apiKeys.metricActive")}
              value={activeCount ?? "—"}
              hint={t("settings.apiKeys.metricActiveHint")}
            />
            <SettingsMetric
              label={t("settings.apiKeys.metricScopes")}
              value={ALL_SCOPES.length}
              hint={t("settings.apiKeys.metricScopesHint")}
            />
          </SettingsMetricRow>
        }
      />

      {newKey ? <NewKeyBanner value={newKey} onDismiss={() => setNewKey("")} /> : null}

      <Alert severity="info" icon={<ShieldOutlinedIcon />} sx={{ borderRadius: 3 }}>
        {t("settings.apiKeys.plaintextWarning")}
      </Alert>

      <SettingsSection
        title={t("settings.apiKeys.sectionKeys")}
        description={t("settings.apiKeys.sectionKeysDesc")}
      >
        <Stack spacing={2}>
          {rows.map((k) => (
            <Box
              key={k.id}
              sx={{
                p: 2.5,
                borderRadius: 3,
                border: 1,
                borderColor: "divider",
                bgcolor: "background.default",
              }}
            >
              <Grid container spacing={2} sx={{ alignItems: "center" }}>
                <Grid size={{ xs: 12, md: 7 }}>
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start" }}>
                    <Box
                      sx={{
                        width: 44,
                        height: 44,
                        borderRadius: 2.5,
                        display: "grid",
                        placeItems: "center",
                        bgcolor: "primary.main",
                        color: "primary.contrastText",
                        flexShrink: 0,
                      }}
                    >
                      <KeyOutlinedIcon fontSize="small" />
                    </Box>
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                      <Stack
                        direction="row"
                        spacing={1}
                        sx={{ alignItems: "center", flexWrap: "wrap", mb: 0.5 }}
                      >
                        <Typography variant="subtitle1" sx={{ fontWeight: 650 }}>
                          {k.name}
                        </Typography>
                        {!k.enabled ? (
                          <Chip
                            label={t("common.statusDisabled")}
                            size="small"
                            variant="outlined"
                          />
                        ) : null}
                      </Stack>
                      <Typography
                        component="code"
                        variant="body2"
                        sx={{ fontFamily: "monospace", color: "text.secondary" }}
                      >
                        {k.keyPrefix}•••••••
                      </Typography>
                      <Stack
                        direction="row"
                        spacing={0.75}
                        sx={{ mt: 1.25, flexWrap: "wrap", gap: 0.75 }}
                      >
                        {scopesOf(k).map((s) => (
                          <Chip
                            key={s}
                            label={s}
                            size="small"
                            color={SCOPE_CHIP_COLOR[s] ?? "default"}
                            variant="outlined"
                          />
                        ))}
                      </Stack>
                    </Box>
                  </Stack>
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    {t("common.created")}{" "}
                    <Box component="span" sx={{ color: "text.primary", fontWeight: 500 }}>
                      {k.createdAt}
                    </Box>
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    {t("settings.apiKeys.used")}{" "}
                    <Box component="span" sx={{ color: "text.primary", fontWeight: 500 }}>
                      {k.lastUsedAt ?? t("common.never")}
                    </Box>
                  </Typography>
                </Grid>
                <Grid
                  size={{ xs: 12, md: 1 }}
                  sx={{ display: "flex", justifyContent: { md: "flex-end" } }}
                >
                  {canDelete ? (
                    <IconButton
                      color="error"
                      aria-label={t("common.delete")}
                      onClick={() =>
                        void deleteMutation
                          .mutateAsync(k.id)
                          .then(() => {
                            void reload();
                            void reloadSummary();
                          })
                          .catch(() => {})
                      }
                    >
                      <DeleteOutlinedIcon />
                    </IconButton>
                  ) : null}
                </Grid>
              </Grid>
            </Box>
          ))}
          {!loading && total === 0 ? (
            <Typography variant="body1" color="text.secondary" sx={{ textAlign: "center", py: 6 }}>
              {t("settings.apiKeys.empty")}
            </Typography>
          ) : null}
        </Stack>
        <Box sx={{ mt: 3 }}>
          <Pagination
            page={page}
            pageSize={pageSize}
            total={total}
            onPage={setPage}
            onPageSize={(size: number) => {
              setPageSize(size);
              setPage(1);
            }}
          />
        </Box>
      </SettingsSection>
    </SettingsWorkspace>
  );
}

export default ApiKeys;
