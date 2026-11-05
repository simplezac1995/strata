// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Divider, Grid, Stack, Typography } from "@mui/material";
import { Button, PageHeader, Switch, TextField } from "@/components/ui/index";
import { useApp } from "@/context/AppContext";
import { useI18n } from "@/shared/i18n";
import { siteSettingApi } from "@/shared/api/system";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { toast } from "@/shared/pageKit/Toast";
import { useAuthStore } from "@/shared/auth/store";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

const BOOL_KEYS = [
  "i18nEnabled",
  "darkModeEnabled",
  "tagsViewEnabled",
  "fullscreenEnabled",
  "lockScreenEnabled",
  "antiDebugEnabled",
  "watermarkEnabled",
  "tenantEnabled",
  "captchaEnabled",
  "loginLockEnabled",
  "aiPackEnabled",
] as const;

type BoolKey = (typeof BOOL_KEYS)[number];

function parseBool(v: string | undefined, fallback: boolean) {
  if (v === undefined) return fallback;
  return v === "true" || v === "1";
}

export function SiteSettings() {
  const { t } = useI18n();
  const { aiPackEnabled, setAiPackEnabled } = useApp();
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:sitesetting:update"));
  const [loading, setLoading] = useState(true);
  const [bools, setBools] = useState<Record<BoolKey, boolean>>({
    i18nEnabled: true,
    darkModeEnabled: true,
    tagsViewEnabled: true,
    fullscreenEnabled: true,
    lockScreenEnabled: true,
    antiDebugEnabled: false,
    watermarkEnabled: false,
    tenantEnabled: false,
    captchaEnabled: true,
    loginLockEnabled: true,
    aiPackEnabled: true,
  });
  const [lockScreenIdleMinutes, setLockScreenIdleMinutes] = useState(30);
  const [maxLoginAttempts, setMaxLoginAttempts] = useState(5);
  const [passwordMinLength, setPasswordMinLength] = useState(8);

  const saveMutation = useMutationWithToast(
    (body: Record<string, string>) => siteSettingApi.putAll(body),
    { success: "toast.saved", failure: "toast.saveFailed" },
  );

  useEffect(() => {
    let cancelled = false;
    siteSettingApi
      .list()
      .then((map) => {
        if (cancelled) return;
        const next = { ...bools };
        for (const k of BOOL_KEYS) {
          next[k] = parseBool(map[k], next[k]);
        }
        setBools(next);
        if (map.lockScreenIdleMinutes) setLockScreenIdleMinutes(Number(map.lockScreenIdleMinutes));
        if (map.maxLoginAttempts) setMaxLoginAttempts(Number(map.maxLoginAttempts));
        if (map.passwordMinLength) setPasswordMinLength(Number(map.passwordMinLength));
        if (map.aiPackEnabled !== undefined) setAiPackEnabled(parseBool(map.aiPackEnabled, true));
      })
      .catch(() => toast.error("toast.loadFailed"))
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- load once
  }, []);

  async function handleSave() {
    const body: Record<string, string> = {
      lockScreenIdleMinutes: String(lockScreenIdleMinutes),
      maxLoginAttempts: String(maxLoginAttempts),
      passwordMinLength: String(passwordMinLength),
    };
    for (const k of BOOL_KEYS) {
      body[k] = String(k === "aiPackEnabled" ? aiPackEnabled : bools[k]);
    }
    try {
      await saveMutation.mutateAsync(body);
    } catch {
      // toast already shown
    }
  }

  const sections = [
    {
      title: "settings.site.sectionUx",
      items: [
        {
          key: "i18nEnabled" as BoolKey,
          label: "system.common.internationalization",
          desc: "settings.site.i18nDesc",
        },
        {
          key: "darkModeEnabled" as BoolKey,
          label: "system.siteSettings.darkMode",
          desc: "settings.site.darkModeDesc",
        },
        {
          key: "tagsViewEnabled" as BoolKey,
          label: "settings.site.tagsView",
          desc: "settings.site.tagsViewDesc",
        },
        {
          key: "fullscreenEnabled" as BoolKey,
          label: "settings.site.fullscreen",
          desc: "settings.site.fullscreenDesc",
        },
        {
          key: "lockScreenEnabled" as BoolKey,
          label: "system.siteSettings.lockScreen",
          desc: "settings.site.lockScreenDesc",
        },
        {
          key: "antiDebugEnabled" as BoolKey,
          label: "settings.site.antiDebug",
          desc: "settings.site.antiDebugDesc",
        },
        {
          key: "watermarkEnabled" as BoolKey,
          label: "system.siteSettings.watermark",
          desc: "settings.site.watermarkDesc",
        },
        {
          key: "tenantEnabled" as BoolKey,
          label: "settings.site.multiTenant",
          desc: "settings.site.multiTenantDesc",
        },
        {
          key: "aiPackEnabled" as const,
          label: "system.common.aiPack",
          desc: "settings.site.aiPackDesc",
        },
      ],
    },
    {
      title: "settings.site.sectionSecurity",
      items: [
        {
          key: "captchaEnabled" as BoolKey,
          label: "settings.site.captcha",
          desc: "settings.site.captchaDesc",
        },
        {
          key: "loginLockEnabled" as BoolKey,
          label: "settings.site.loginLock",
          desc: "settings.site.loginLockDesc",
        },
      ],
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("settings.site.title")}
        subtitle={loading ? t("common.loading") : t("settings.site.subtitle")}
        actions={
          <Button variant="contained" disabled={!canUpdate} onClick={() => void handleSave()}>
            {t("common.saveChanges")}
          </Button>
        }
      />

      <Grid container spacing={2.5}>
        {sections.map((section) => (
          <Grid key={section.title} size={{ xs: 12, lg: 6 }}>
            <ContentCard padding={false}>
              <Box sx={{ px: 2.5, py: 2, borderBottom: 1, borderColor: "divider" }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {t(section.title)}
                </Typography>
              </Box>
              <Stack divider={<Divider />}>
                {section.items.map((item) => (
                  <Stack
                    key={item.key}
                    direction="row"
                    sx={{
                      alignItems: "center",
                      justifyContent: "space-between",
                      px: 2.5,
                      py: 1.75,
                    }}
                  >
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {t(item.label)}
                      </Typography>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ display: "block", mt: 0.25 }}
                      >
                        {t(item.desc)}
                      </Typography>
                    </Box>
                    <Switch
                      checked={
                        item.key === "aiPackEnabled" ? aiPackEnabled : bools[item.key as BoolKey]
                      }
                      disabled={!canUpdate}
                      onChange={(v) => {
                        if (item.key === "aiPackEnabled") setAiPackEnabled(v);
                        else setBools((s) => ({ ...s, [item.key]: v }));
                      }}
                    />
                  </Stack>
                ))}
              </Stack>
            </ContentCard>
          </Grid>
        ))}

        <Grid size={{ xs: 12, lg: 6 }}>
          <ContentCard padding={false}>
            <Box sx={{ px: 2.5, py: 2, borderBottom: 1, borderColor: "divider" }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {t("system.siteSettings.thresholds")}
              </Typography>
            </Box>
            <Stack spacing={2} sx={{ p: 2.5 }}>
              <TextField
                label={t("system.siteSettings.lockScreenIdleMinutes")}
                type="number"
                value={String(lockScreenIdleMinutes)}
                onChange={(e) => setLockScreenIdleMinutes(Number(e.target.value))}
                hint={t("system.siteSettings.minutesOfInactivityBeforeAutoLock")}
              />
              <TextField
                label={t("system.siteSettings.maxLoginAttempts")}
                type="number"
                value={String(maxLoginAttempts)}
                onChange={(e) => setMaxLoginAttempts(Number(e.target.value))}
                hint={t("system.siteSettings.failedLoginsBeforeAccountLock")}
              />
              <TextField
                label={t("system.siteSettings.passwordMinLength")}
                type="number"
                value={String(passwordMinLength)}
                onChange={(e) => setPasswordMinLength(Number(e.target.value))}
              />
            </Stack>
          </ContentCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 6 }}>
          <ContentCard padding={false}>
            <Box sx={{ px: 2.5, py: 2, borderBottom: 1, borderColor: "divider" }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {t("system.siteSettings.about")}
              </Typography>
            </Box>
            <Stack spacing={1.5} sx={{ p: 2.5 }}>
              {[
                ["settings.site.aboutProduct", "Strata Admin"],
                ["settings.site.aboutVersion", "0.1.0-SNAPSHOT"],
                ["settings.site.aboutLicense", "Apache-2.0"],
                ["settings.site.aboutStack", t("settings.site.aboutStackValue")],
              ].map(([k, v]) => (
                <Stack key={k} direction="row" sx={{ justifyContent: "space-between" }}>
                  <Typography variant="body2" color="text.secondary">
                    {t(k)}
                  </Typography>
                  <Typography variant="caption" sx={{ fontWeight: 600, fontFamily: "monospace" }}>
                    {v}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </ContentCard>
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default SiteSettings;
