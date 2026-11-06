// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import MonitorOutlinedIcon from "@mui/icons-material/MonitorOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import { Box, Grid, Stack, Typography } from "@mui/material";
import type { SvgIconComponent } from "@mui/icons-material";
import { useQuery } from "@tanstack/react-query";
import { Button, Switch } from "@/components/ui/index";
import { notificationPrefApi } from "@/shared/api/system";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { useI18n } from "@/shared/i18n";
import {
  PageHero as SettingsHero,
  PageSection as SettingsSection,
  PageWorkspace as SettingsWorkspace,
} from "@/shared/pageKit/PageWorkspace";
import type { NotificationPrefDto } from "@/shared/types";

const ROWS: {
  key: keyof NotificationPrefDto;
  label: string;
  desc: string;
  Icon: SvgIconComponent;
}[] = [
  {
    key: "emailEnabled",
    label: "settings.notifications.email",
    desc: "settings.notifications.emailDesc",
    Icon: EmailOutlinedIcon,
  },
  {
    key: "inAppEnabled",
    label: "settings.notifications.inApp",
    desc: "settings.notifications.inAppDesc",
    Icon: MonitorOutlinedIcon,
  },
  {
    key: "securityEnabled",
    label: "settings.notifications.security",
    desc: "settings.notifications.securityDesc",
    Icon: ShieldOutlinedIcon,
  },
  {
    key: "marketingEnabled",
    label: "settings.notifications.marketing",
    desc: "settings.notifications.marketingDesc",
    Icon: NotificationsOutlinedIcon,
  },
];

function NotificationCard({
  label,
  desc,
  Icon,
  checked,
  disabled,
  onChange,
}: {
  label: string;
  desc: string;
  Icon: SvgIconComponent;
  checked: boolean;
  disabled: boolean;
  onChange: (value: boolean) => void;
}) {
  const { t } = useI18n();
  return (
    <Box
      sx={{
        p: 2.5,
        height: "100%",
        borderRadius: 3,
        border: 1,
        borderColor: checked ? "primary.light" : "divider",
        bgcolor: checked ? "action.hover" : "background.default",
        display: "flex",
        flexDirection: "column",
        gap: 2,
      }}
    >
      <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start" }}>
        <Box
          sx={{
            width: 44,
            height: 44,
            borderRadius: 2.5,
            display: "grid",
            placeItems: "center",
            bgcolor: "background.paper",
            border: 1,
            borderColor: "divider",
            flexShrink: 0,
          }}
        >
          <Icon sx={{ fontSize: 22, color: "primary.main" }} />
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 650 }}>
            {t(label)}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75, lineHeight: 1.6 }}>
            {t(desc)}
          </Typography>
        </Box>
      </Stack>
      <Stack
        direction="row"
        sx={{ justifyContent: "space-between", alignItems: "center", mt: "auto" }}
      >
        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
          {checked ? t("common.enabled") : t("common.disabled")}
        </Typography>
        <Switch checked={checked} disabled={disabled} onChange={onChange} />
      </Stack>
    </Box>
  );
}

export function NotificationSettings() {
  const { t } = useI18n();
  const [prefs, setPrefs] = useState<NotificationPrefDto>({
    emailEnabled: true,
    inAppEnabled: true,
    marketingEnabled: false,
    securityEnabled: true,
  });
  const [saved, setSaved] = useState(false);

  const { data: prefsData, isLoading: loading } = useQuery({
    queryKey: ["api", "system", "notification-prefs"],
    queryFn: () => notificationPrefApi.get(),
  });

  useEffect(() => {
    if (prefsData) setPrefs(prefsData);
  }, [prefsData]);

  const saveMutation = useMutationWithToast(
    (payload: NotificationPrefDto) => notificationPrefApi.update(payload),
    { success: "toast.saved", failure: "toast.saveFailed" },
    {
      onSuccess: () => {
        setSaved(true);
        setTimeout(() => setSaved(false), 1800);
      },
    },
  );

  const enabledCount = [
    prefs.emailEnabled,
    prefs.inAppEnabled,
    prefs.securityEnabled,
    prefs.marketingEnabled,
  ].filter(Boolean).length;

  return (
    <SettingsWorkspace>
      <SettingsHero
        title={t("settings.notifications.pageTitle")}
        subtitle={t("settings.notifications.pageSubtitle")}
        actions={
          <Button
            variant="contained"
            onClick={() => void saveMutation.mutateAsync(prefs).catch(() => {})}
            disabled={loading}
          >
            {saved ? t("common.saved") : t("common.saveChanges")}
          </Button>
        }
        footer={
          <Typography variant="body2" color="text.secondary">
            {loading
              ? t("common.loading")
              : `${enabledCount} / ${ROWS.length} ${t("settings.notifications.channelsEnabled")}`}
          </Typography>
        }
      />

      <SettingsSection
        title={t("settings.notifications.sectionChannels")}
        description={t("settings.notifications.sectionChannelsDesc")}
      >
        <Grid container spacing={2.5}>
          {ROWS.map((row) => (
            <Grid key={row.key} size={{ xs: 12, md: 6 }}>
              <NotificationCard
                label={row.label}
                desc={row.desc}
                Icon={row.Icon}
                checked={prefs[row.key]}
                disabled={loading}
                onChange={(value) => setPrefs((p) => ({ ...p, [row.key]: value }))}
              />
            </Grid>
          ))}
        </Grid>
      </SettingsSection>
    </SettingsWorkspace>
  );
}

export default NotificationSettings;
