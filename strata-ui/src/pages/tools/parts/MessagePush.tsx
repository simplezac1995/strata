// SPDX-License-Identifier: Apache-2.0
import { useEffect, useMemo, useState } from "react";
import { Box, Button as MuiButton, Grid, Stack, Typography } from "@mui/material";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import PhoneAndroidOutlinedIcon from "@mui/icons-material/PhoneAndroidOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import type { SvgIconComponent } from "@mui/icons-material";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { Button, PageHeader, TextField, Switch, TextArea, Pagination } from "@/components/ui/index";
import { messageChannelApi } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

type ChannelId = "email" | "sms" | "webhook" | "internal";

interface EmailConfig {
  host?: string;
  port?: string;
  encryption?: string;
  username?: string;
  password?: string;
  fromName?: string;
}

function parseConfig(json: string | null): Record<string, string> {
  if (!json) return {};
  try {
    const parsed = JSON.parse(json) as Record<string, string>;
    return parsed ?? {};
  } catch {
    return {};
  }
}

export function MessagePush() {
  const { t } = useI18n();
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:messagechannel:update"));

  const {
    rows: channels,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
  } = usePagedQuery(["api", "system", "message-channels"], messageChannelApi.page);

  const [activeChannel, setActiveChannel] = useState<ChannelId>("email");
  const [enabled, setEnabled] = useState(false);
  const [emailConfig, setEmailConfig] = useState<EmailConfig>({});
  const [configJson, setConfigJson] = useState("{}");

  const updateMutation = useMutationWithToast(
    ({ channel, body, enabled }: { channel: ChannelId; body: string; enabled: boolean }) =>
      messageChannelApi.update(channel, body, enabled),
    { success: "toast.saved", failure: "toast.saveFailed" },
    { onSuccess: () => reload() },
  );

  const channelMeta: { id: ChannelId; Icon: SvgIconComponent; labelKey: string }[] = useMemo(
    () => [
      { id: "email", Icon: EmailOutlinedIcon, labelKey: "tools.messagePush.channelEmailSmtp" },
      { id: "sms", Icon: PhoneAndroidOutlinedIcon, labelKey: "tools.messagePush.channelSms" },
      { id: "webhook", Icon: LinkOutlinedIcon, labelKey: "tools.messagePush.channelWebhook" },
      {
        id: "internal",
        Icon: NotificationsOutlinedIcon,
        labelKey: "tools.messagePush.channelInternalPush",
      },
    ],
    [],
  );

  const activeDto = channels.find((c) => c.channelType === activeChannel);

  useEffect(() => {
    if (!activeDto) return;
    setEnabled(activeDto.enabled);
    const parsed = parseConfig(activeDto.configJson);
    if (activeChannel === "email") {
      setEmailConfig({
        host: parsed.host ?? "",
        port: parsed.port ?? "587",
        encryption: parsed.encryption ?? "tls",
        username: parsed.username ?? "",
        password: parsed.password ?? "",
        fromName: parsed.fromName ?? "",
      });
    } else {
      setConfigJson(activeDto.configJson ?? "{}");
    }
  }, [activeDto, activeChannel]);

  async function handleSave() {
    if (!activeDto) return;
    const body =
      activeChannel === "email"
        ? JSON.stringify({
            host: emailConfig.host ?? "",
            port: emailConfig.port ?? "",
            encryption: emailConfig.encryption ?? "tls",
            username: emailConfig.username ?? "",
            password: emailConfig.password ?? "",
            fromName: emailConfig.fromName ?? "",
          })
        : configJson;
    try {
      await updateMutation.mutateAsync({ channel: activeChannel, body, enabled });
    } catch {
      // toast already shown
    }
  }

  const activeLabel = channelMeta.find((c) => c.id === activeChannel)?.labelKey;

  return (
    <PageStack>
      <PageHeader
        title={t("tools.messagePush.messagePush")}
        subtitle={t("tools.messagePush.configureNotificationChannels")}
      />
      {loading ? (
        <Typography variant="body2" color="text.secondary">
          {t("common.loading")}
        </Typography>
      ) : (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 3 }}>
            <Stack spacing={0.5}>
              {channelMeta.map((c) => (
                <MuiButton
                  key={c.id}
                  variant={activeChannel === c.id ? "contained" : "text"}
                  color={activeChannel === c.id ? "primary" : "inherit"}
                  onClick={() => setActiveChannel(c.id)}
                  sx={{
                    justifyContent: "flex-start",
                    textTransform: "none",
                    borderRadius: 3,
                    py: 1.25,
                  }}
                >
                  <Box
                    component="span"
                    sx={{ mr: 1, display: "inline-flex", verticalAlign: "middle" }}
                  >
                    <c.Icon sx={{ fontSize: 18 }} />
                  </Box>
                  {t(c.labelKey)}
                </MuiButton>
              ))}
            </Stack>
          </Grid>
          <Grid size={{ xs: 12, md: 9 }}>
            <ContentCard>
              <Stack
                direction="row"
                sx={{ mb: 2.5, justifyContent: "space-between", alignItems: "center" }}
              >
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {activeLabel ? t(activeLabel) : activeChannel}
                </Typography>
                <Switch
                  label={t("common.enabled")}
                  checked={enabled}
                  onChange={setEnabled}
                  disabled={!canUpdate}
                />
              </Stack>
              {activeChannel === "email" ? (
                <Stack spacing={2}>
                  <TextField
                    label={t("tools.messagePush.smtpHost")}
                    value={emailConfig.host ?? ""}
                    onChange={(e) => setEmailConfig((c) => ({ ...c, host: e.target.value }))}
                  />
                  <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 6 }}>
                      <TextField
                        label={t("tools.messagePush.port")}
                        value={emailConfig.port ?? ""}
                        onChange={(e) => setEmailConfig((c) => ({ ...c, port: e.target.value }))}
                      />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                      <TextField
                        label={t("tools.messagePush.encryption")}
                        value={emailConfig.encryption ?? ""}
                        onChange={(e) =>
                          setEmailConfig((c) => ({ ...c, encryption: e.target.value }))
                        }
                      />
                    </Grid>
                  </Grid>
                  <TextField
                    label={t("common.username")}
                    value={emailConfig.username ?? ""}
                    onChange={(e) => setEmailConfig((c) => ({ ...c, username: e.target.value }))}
                  />
                  <TextField
                    label={t("common.password")}
                    type="password"
                    value={emailConfig.password ?? ""}
                    onChange={(e) => setEmailConfig((c) => ({ ...c, password: e.target.value }))}
                  />
                  <TextField
                    label={t("tools.messagePush.fromName")}
                    value={emailConfig.fromName ?? ""}
                    onChange={(e) => setEmailConfig((c) => ({ ...c, fromName: e.target.value }))}
                  />
                </Stack>
              ) : (
                <TextArea
                  label={t("tools.messagePush.configJson")}
                  rows={8}
                  value={configJson}
                  onChange={(e) => setConfigJson(e.target.value)}
                  disabled={!canUpdate}
                  slotProps={{ input: { sx: { fontFamily: "monospace", fontSize: 12 } } }}
                />
              )}
              {canUpdate && (
                <Stack direction="row" spacing={1} sx={{ mt: 3 }}>
                  <Button variant="contained" onClick={() => void handleSave()}>
                    {t("common.save")}
                  </Button>
                </Stack>
              )}
            </ContentCard>
          </Grid>
        </Grid>
      )}
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
    </PageStack>
  );
}

export default MessagePush;
