// SPDX-License-Identifier: Apache-2.0
import { Avatar, Box, ButtonBase, Grid, Typography } from "@mui/material";
import CheckIcon from "@mui/icons-material/Check";
import { useAppearance } from "@/context/AppearanceContext";
import { useI18n } from "@/shared/i18n";
import {
  PageHero as SettingsHero,
  PageSection as SettingsSection,
  PageWorkspace as SettingsWorkspace,
} from "@/shared/pageKit/PageWorkspace";

export function Language() {
  const { lang, setLang } = useAppearance();
  const { t } = useI18n();
  const LANGS = [
    {
      code: "zh" as const,
      label: "settings.language.zhLabel",
      native: "settings.language.zhNative",
      abbreviation: "settings.language.zhAbbr",
      region: "settings.language.regionChina",
    },
    {
      code: "en" as const,
      label: "settings.language.enLabel",
      native: "English",
      abbreviation: "EN",
      region: "settings.language.regionUs",
    },
  ];
  return (
    <SettingsWorkspace>
      <SettingsHero title={t("nav.language")} subtitle={t("settings.language.subtitle")} />
      <SettingsSection
        title={t("settings.language.sectionTitle")}
        description={t("settings.language.sectionDesc")}
      >
        <Grid container spacing={2.5}>
          {LANGS.map((l) => {
            const active = l.code === lang;
            return (
              <Grid key={l.code} size={{ xs: 12, md: 6 }}>
                <ButtonBase
                  onClick={() => setLang(l.code)}
                  sx={{
                    width: "100%",
                    display: "flex",
                    alignItems: "center",
                    gap: 2,
                    p: 2.5,
                    textAlign: "left",
                    borderRadius: 3,
                    border: 2,
                    borderColor: active ? "primary.main" : "divider",
                    bgcolor: active ? "action.hover" : "background.default",
                    "&:hover": { borderColor: active ? "primary.main" : "text.secondary" },
                  }}
                >
                  <Typography
                    component="span"
                    sx={{ fontSize: 18, fontWeight: 700, lineHeight: 1 }}
                  >
                    {t(l.abbreviation)}
                  </Typography>
                  <Box sx={{ flex: 1 }}>
                    <Typography
                      variant="subtitle1"
                      sx={{ fontWeight: 650 }}
                      color={active ? "primary.main" : "text.primary"}
                    >
                      {l.code === "en" ? l.native : t(l.native)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      {t(l.label)} · {t(l.region)}
                    </Typography>
                  </Box>
                  {active ? (
                    <Avatar sx={{ width: 28, height: 28, bgcolor: "primary.main" }}>
                      <CheckIcon sx={{ fontSize: 16 }} />
                    </Avatar>
                  ) : null}
                </ButtonBase>
              </Grid>
            );
          })}
        </Grid>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2.5 }}>
          {t("settings.language.footer")}
        </Typography>
      </SettingsSection>
    </SettingsWorkspace>
  );
}

export default Language;
