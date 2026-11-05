// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import {
  Avatar,
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import DarkModeOutlinedIcon from "@mui/icons-material/DarkModeOutlined";
import LightModeOutlinedIcon from "@mui/icons-material/LightModeOutlined";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import { useState, type MouseEvent } from "react";
import { useAppearance } from "@/context/AppearanceContext";
import type { Lang } from "@/context/AppearanceContext";
import { useI18n } from "@/shared/i18n";
import { authFormPaperSx, authHeroPanelSx } from "@/theme/pageStyles";

const LANGS: { code: Lang; native: string; abbreviation: string }[] = [
  { code: "zh", native: "settings.language.zhNative", abbreviation: "settings.language.zhAbbr" },
  { code: "en", native: "English", abbreviation: "EN" },
];

export function AuthFormCard({
  children,
  align = "left",
}: {
  children: ReactNode;
  align?: "left" | "center";
}) {
  return (
    <Paper
      elevation={0}
      sx={(theme) => ({
        ...authFormPaperSx(theme),
        textAlign: align,
        ...(align === "center" ? { "& .MuiButton-root": { justifyContent: "center" } } : {}),
      })}
    >
      {children}
    </Paper>
  );
}

export function AuthToolbar() {
  const { dark, toggleDark, lang, setLang } = useAppearance();
  const { t } = useI18n();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const current = LANGS.find((l) => l.code === lang) ?? LANGS[0];

  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
      <IconButton
        size="small"
        aria-label={t("common.selectLanguage")}
        onClick={(e: MouseEvent<HTMLElement>) => setAnchor(e.currentTarget)}
        sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, px: 1, gap: 0.5 }}
      >
        <Typography component="span" sx={{ fontSize: 14 }}>
          {t(current.abbreviation)}
        </Typography>
        <Typography variant="caption" sx={{ fontWeight: 600 }}>
          {current.code.toUpperCase()}
        </Typography>
        <KeyboardArrowDownIcon sx={{ fontSize: 14 }} />
      </IconButton>
      <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={() => setAnchor(null)}>
        {LANGS.map((l) => (
          <MenuItem
            key={l.code}
            selected={l.code === lang}
            onClick={() => {
              setLang(l.code);
              setAnchor(null);
            }}
          >
            <Typography sx={{ mr: 1 }}>{t(l.abbreviation)}</Typography>
            {l.code === "en" ? l.native : t(l.native)}
          </MenuItem>
        ))}
      </Menu>
      <IconButton
        size="small"
        onClick={toggleDark}
        title={dark ? t("shell.theme.light") : t("shell.theme.dark")}
        aria-label={dark ? t("shell.theme.light") : t("shell.theme.dark")}
        sx={{ border: 1, borderColor: "divider", borderRadius: 1.5 }}
      >
        {dark ? (
          <LightModeOutlinedIcon fontSize="small" />
        ) : (
          <DarkModeOutlinedIcon fontSize="small" />
        )}
      </IconButton>
    </Stack>
  );
}

export function AuthLayout({
  heroTitle,
  heroSubtitle,
  heroIcon,
  heroBullets,
  heroTags,
  children,
}: {
  heroTitle: ReactNode;
  heroSubtitle?: ReactNode;
  heroIcon?: ReactNode;
  heroBullets?: ReactNode[];
  heroTags?: string[];
  children: ReactNode;
}) {
  const { t } = useI18n();

  return (
    <Box sx={{ minHeight: "100vh", display: "flex", bgcolor: "background.default" }}>
      <Box
        sx={(theme) => ({
          display: { xs: "none", lg: "flex" },
          width: "52%",
          position: "relative",
          overflow: "hidden",
          flexDirection: "column",
          ...authHeroPanelSx(theme),
        })}
      >
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            opacity: 0.07,
            backgroundImage: "radial-gradient(circle, white 1px, transparent 1px)",
            backgroundSize: "28px 28px",
          }}
        />
        <Stack direction="row" spacing={1.5} sx={{ position: "relative", zIndex: 1, px: 6, pt: 5 }}>
          <Avatar sx={{ width: 36, height: 36, bgcolor: "rgba(255,255,255,0.2)", fontWeight: 700 }}>
            S
          </Avatar>
          <Box>
            <Typography variant="subtitle2" sx={{ color: "#fff" }}>
              {t("auth.brand.title")}
            </Typography>
            <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.55)" }}>
              {t("auth.brand.tagline")}
            </Typography>
          </Box>
        </Stack>
        <Stack
          sx={{
            position: "relative",
            zIndex: 1,
            flex: 1,
            justifyContent: "center",
            px: 6,
            pb: 2,
          }}
          spacing={3}
        >
          {heroIcon}
          <Box>
            <Typography
              component="h1"
              variant="h2"
              sx={{
                color: "#fff",
                fontSize: { lg: "2.25rem", xl: "2.625rem" },
                lineHeight: 1.15,
                mb: 2,
              }}
            >
              {heroTitle}
            </Typography>
            {heroSubtitle ? (
              <Typography variant="body1" sx={{ color: "rgba(255,255,255,0.7)", maxWidth: 400 }}>
                {heroSubtitle}
              </Typography>
            ) : null}
          </Box>
          {heroBullets?.length ? <Stack spacing={1.5}>{heroBullets}</Stack> : null}
          {heroTags?.length ? (
            <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap", gap: 1 }}>
              {heroTags.map((tag) => (
                <Chip
                  key={tag}
                  label={t(tag)}
                  size="small"
                  sx={{
                    bgcolor: "rgba(255,255,255,0.1)",
                    color: "rgba(255,255,255,0.8)",
                    border: "1px solid rgba(255,255,255,0.15)",
                  }}
                />
              ))}
            </Stack>
          ) : null}
        </Stack>
        <Typography
          variant="caption"
          sx={{
            position: "relative",
            zIndex: 1,
            color: "rgba(255,255,255,0.35)",
            px: 6,
            pb: 4,
          }}
        >
          © 2026 Strata Admin · Open Core · Apache 2.0
        </Typography>
      </Box>

      <Box sx={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <Stack
          direction="row"
          sx={{
            alignItems: "center",
            justifyContent: "space-between",
            px: { xs: 2, sm: 4 },
            py: 2,
          }}
        >
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: "center", display: { xs: "flex", lg: "none" } }}
          >
            <Avatar sx={{ width: 28, height: 28, bgcolor: "primary.main", fontSize: 12 }}>S</Avatar>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              {t("auth.brand.title")}
            </Typography>
          </Stack>
          <Box sx={{ display: { xs: "none", lg: "block" } }} />
          <AuthToolbar />
        </Stack>
        <Box
          sx={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "flex-start",
            pt: { xs: 2, sm: 6 },
            px: { xs: 2, sm: 4 },
            pb: 4,
            overflowY: "auto",
          }}
        >
          <Box sx={{ width: "100%", maxWidth: 400, flexShrink: 0 }}>{children}</Box>
        </Box>
      </Box>
    </Box>
  );
}
