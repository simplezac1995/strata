// SPDX-License-Identifier: Apache-2.0
import {
  Box,
  Divider,
  Drawer,
  IconButton,
  Stack,
  Typography,
  Button as MuiButton,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import LayoutTemplateIcon from "@mui/icons-material/ViewQuiltOutlined";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import { useApp } from "@/context/AppContext";
import { useAppearance } from "@/context/AppearanceContext";
import { useI18n } from "@/shared/i18n";
import { Alert } from "@/components/ui/index";
import { toast } from "@/shared/pageKit/Toast";
import {
  ConfigSection,
  ConfigSwitchRow,
  LayoutModePicker,
  SidebarThemePicker,
  CustomSidebarColorsPanel,
  DensityPicker,
} from "@/components/shell/layoutConfig";
import { configListSx } from "@/theme/shellStyles";

const DRAWER_WIDTH = 360;

export function LayoutDrawer() {
  const { t } = useI18n();
  const {
    layoutDrawerOpen,
    setLayoutDrawerOpen,
    layoutMode,
    setLayoutMode,
    menuAccordion,
    setMenuAccordion,
    fixedHeader,
    setFixedHeader,
    showBreadcrumb,
    setShowBreadcrumb,
    showTagsview,
    setShowTagsview,
    showSidebarLogo,
    setShowSidebarLogo,
    grayMode,
    setGrayMode,
    colorWeakMode,
    setColorWeakMode,
    watermark,
    setWatermark,
    topbarGradient,
    setTopbarGradient,
    menuGradient,
    setMenuGradient,
    lockScreenEnabled,
    setLockScreenEnabled,
  } = useApp();
  const {
    sidebarTheme,
    setSidebarTheme,
    customSidebarBg,
    setCustomSidebarBg,
    customSidebarAccent,
    setCustomSidebarAccent,
    dark,
    toggleDark,
    density,
    setDensity,
  } = useAppearance();

  function handleCopyConfig() {
    const cfg = JSON.stringify(
      {
        layoutMode,
        sidebarTheme,
        dark,
        density,
        menuAccordion,
        fixedHeader,
        showBreadcrumb,
        showTagsview,
        showSidebarLogo,
        grayMode,
        colorWeakMode,
        watermark,
        topbarGradient,
        menuGradient,
        lockScreenEnabled,
      },
      null,
      2,
    );
    void navigator.clipboard?.writeText(cfg).then(() => {
      toast.success("shell.copied");
    });
  }

  return (
    <Drawer
      anchor="right"
      open={layoutDrawerOpen}
      onClose={() => setLayoutDrawerOpen(false)}
      slotProps={{
        paper: {
          sx: {
            width: { xs: "100%", sm: DRAWER_WIDTH },
            maxWidth: "100%",
            display: "flex",
            flexDirection: "column",
          },
        },
      }}
    >
      <Stack
        direction="row"
        sx={{
          px: 2.5,
          py: 2,
          borderBottom: 1,
          borderColor: "divider",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <Stack direction="row" spacing={1} sx={{ alignItems: "center", minWidth: 0 }}>
          <LayoutTemplateIcon color="primary" fontSize="small" />
          <Typography variant="subtitle2" noWrap>
            {t("shell.layout.config")}
          </Typography>
        </Stack>
        <IconButton
          size="small"
          onClick={() => setLayoutDrawerOpen(false)}
          aria-label={t("shell.search.close")}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </Stack>

      <Box sx={{ flex: 1, overflow: "auto", px: 2.5, pb: 2.5 }}>
        <ConfigSection label={t("shell.layout.title")}>
          <LayoutModePicker value={layoutMode} onChange={setLayoutMode} t={t} />
        </ConfigSection>

        <ConfigSection label={t("shell.layout.sidebarTheme")}>
          <SidebarThemePicker
            value={sidebarTheme}
            onChange={setSidebarTheme}
            customSidebarBg={customSidebarBg}
            customSidebarAccent={customSidebarAccent}
            compact
          />
          {sidebarTheme === "custom" && (
            <CustomSidebarColorsPanel
              customSidebarBg={customSidebarBg}
              setCustomSidebarBg={setCustomSidebarBg}
              customSidebarAccent={customSidebarAccent}
              setCustomSidebarAccent={setCustomSidebarAccent}
              t={t}
            />
          )}
        </ConfigSection>

        <ConfigSection label={t("shell.layout.global")}>
          <Box sx={configListSx}>
            <ConfigSwitchRow
              label={t("shell.dark.mode")}
              checked={dark}
              onChange={(v) => {
                if (v !== dark) toggleDark();
              }}
            />
            <ConfigSwitchRow
              label={t("shell.layout.topbarGradient")}
              checked={topbarGradient}
              onChange={setTopbarGradient}
            />
            <ConfigSwitchRow
              label={t("shell.layout.menuGradient")}
              checked={menuGradient}
              onChange={setMenuGradient}
            />
          </Box>
        </ConfigSection>

        <ConfigSection label={t("shell.layout.interface")}>
          <Box sx={configListSx}>
            <ConfigSwitchRow
              label={t("shell.layout.menuAccordion")}
              checked={menuAccordion}
              onChange={setMenuAccordion}
            />
            <ConfigSwitchRow
              label={t("shell.layout.fixedHeader")}
              checked={fixedHeader}
              onChange={setFixedHeader}
            />
            <ConfigSwitchRow
              label={t("shell.layout.enableLockScreen")}
              checked={lockScreenEnabled}
              onChange={setLockScreenEnabled}
            />
          </Box>
        </ConfigSection>

        <ConfigSection label={t("shell.layout.display")}>
          <Box sx={configListSx}>
            <ConfigSwitchRow
              label={t("shell.layout.sidebarLogo")}
              checked={showSidebarLogo}
              onChange={setShowSidebarLogo}
            />
            <ConfigSwitchRow
              label={t("shell.layout.breadcrumb")}
              checked={showBreadcrumb}
              onChange={setShowBreadcrumb}
            />
            <ConfigSwitchRow
              label={t("shell.layout.tabsBar")}
              checked={showTagsview}
              onChange={setShowTagsview}
            />
            <ConfigSwitchRow
              label={t("shell.layout.grayMode")}
              checked={grayMode}
              onChange={setGrayMode}
            />
            <ConfigSwitchRow
              label={t("shell.layout.colorWeakMode")}
              checked={colorWeakMode}
              onChange={setColorWeakMode}
            />
            <ConfigSwitchRow
              label={t("shell.layout.watermark")}
              checked={watermark}
              onChange={setWatermark}
            />
          </Box>
        </ConfigSection>

        <ConfigSection label={t("settings.appearance.layoutDensity")}>
          <DensityPicker value={density} onChange={setDensity} t={t} />
        </ConfigSection>

        <Box sx={{ mt: 2.5 }}>
          <Alert type="info">
            <Typography variant="caption" component="span" sx={{ lineHeight: 1.6 }}>
              {t("shell.layout.copyHint")}
            </Typography>
          </Alert>
        </Box>
      </Box>

      <Divider />
      <Box sx={{ p: 2.5 }}>
        <MuiButton
          variant="contained"
          fullWidth
          onClick={handleCopyConfig}
          sx={{ justifyContent: "center" }}
        >
          <ContentCopyIcon sx={{ fontSize: 16, mr: 0.5 }} />
          {t("shell.layout.copyConfig")}
        </MuiButton>
      </Box>
    </Drawer>
  );
}
