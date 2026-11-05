// SPDX-License-Identifier: Apache-2.0
import type { Theme } from "@mui/material/styles";
import { alpha } from "@mui/material/styles";
import { Box } from "@mui/material";
import type { LayoutMode } from "@/context/AppContext";

export {
  ConfigSection,
  ConfigSwitchRow,
  LayoutModePicker,
  SidebarThemePicker,
  CustomSidebarColorsPanel,
  DensityPicker,
} from "./layoutconfig/part-1";

export function LayoutPreview({ mode, theme }: { mode: LayoutMode; theme: Theme }) {
  const border = theme.palette.divider;
  const isDark = theme.palette.mode === "dark";

  const sidebar = isDark ? "#334155" : "#334155";
  const header = isDark ? "#475569" : "#e2e8f0";
  const tabRow = alpha(theme.palette.primary.main, isDark ? 0.25 : 0.08);
  const content = isDark ? "rgba(15,23,42,0.4)" : "#ffffff";
  const topbar = theme.palette.primary.main;

  if (mode === "left") {
    return (
      <Box
        sx={{
          display: "flex",
          width: "100%",
          height: "100%",
          borderRadius: 1,
          overflow: "hidden",
          border: 1,
          borderColor: border,
        }}
      >
        <Box sx={{ width: "30%", bgcolor: sidebar }} />
        <Box sx={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
          <Box sx={{ height: "22%", bgcolor: header, borderBottom: 1, borderColor: border }} />
          <Box sx={{ height: "10%", bgcolor: tabRow, borderBottom: 1, borderColor: border }} />
          <Box sx={{ flex: 1, bgcolor: content }} />
        </Box>
      </Box>
    );
  }

  if (mode === "mix") {
    return (
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          width: "100%",
          height: "100%",
          borderRadius: 1,
          overflow: "hidden",
          border: 1,
          borderColor: border,
        }}
      >
        <Box sx={{ height: "24%", bgcolor: topbar, flexShrink: 0 }} />
        <Box sx={{ display: "flex", flex: 1, minHeight: 0 }}>
          <Box
            sx={{
              width: "28%",
              bgcolor: isDark ? "#1e293b" : "#fff",
              borderRight: 1,
              borderColor: border,
              p: 0.5,
              display: "flex",
              flexDirection: "column",
              gap: 0.5,
            }}
          >
            <Box sx={{ height: 6, borderRadius: 0.5, bgcolor: "rgba(99,102,241,0.7)" }} />
            <Box sx={{ height: 4, borderRadius: 0.5, bgcolor: header }} />
            <Box sx={{ height: 4, borderRadius: 0.5, bgcolor: header }} />
          </Box>
          <Box sx={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
            <Box sx={{ height: "14%", bgcolor: tabRow, borderBottom: 1, borderColor: border }} />
            <Box sx={{ flex: 1, bgcolor: content }} />
          </Box>
        </Box>
      </Box>
    );
  }

  if (mode === "top") {
    return (
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          width: "100%",
          height: "100%",
          borderRadius: 1,
          overflow: "hidden",
          border: 1,
          borderColor: border,
        }}
      >
        <Box sx={{ height: "30%", bgcolor: topbar }} />
        <Box sx={{ height: "10%", bgcolor: tabRow, borderBottom: 1, borderColor: border }} />
        <Box sx={{ flex: 1, bgcolor: content }} />
      </Box>
    );
  }

  return (
    <Box
      sx={{
        display: "flex",
        width: "100%",
        height: "100%",
        borderRadius: 1,
        overflow: "hidden",
        border: 1,
        borderColor: border,
      }}
    >
      <Box sx={{ width: "11%", bgcolor: isDark ? "#0f172a" : "#1e293b" }} />
      <Box
        sx={{
          width: "24%",
          bgcolor: isDark ? "#1e293b" : "#f1f5f9",
          borderRight: 1,
          borderColor: border,
        }}
      />
      <Box sx={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <Box sx={{ height: "20%", bgcolor: header, borderBottom: 1, borderColor: border }} />
        <Box sx={{ height: "9%", bgcolor: tabRow, borderBottom: 1, borderColor: border }} />
        <Box sx={{ flex: 1, bgcolor: content }} />
      </Box>
    </Box>
  );
}
