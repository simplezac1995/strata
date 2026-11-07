// SPDX-License-Identifier: Apache-2.0
import type { SvgIconComponent } from "@mui/icons-material";

/** Renders a nav icon at a pixel size (MUI `fontSize` in px). */
export function NavIcon({ icon: Icon, size }: { icon: SvgIconComponent; size: number }) {
  return <Icon sx={{ fontSize: size, display: "block" }} />;
}
