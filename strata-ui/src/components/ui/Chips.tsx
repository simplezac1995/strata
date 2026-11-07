// SPDX-License-Identifier: Apache-2.0
import { Avatar as MuiAvatar, Chip as MuiChip, Tooltip as MuiTooltip } from "@mui/material";
import type {
  AvatarProps as MuiAvatarProps,
  ChipProps as MuiChipProps,
  TooltipProps as MuiTooltipProps,
} from "@mui/material";
import { useI18n } from "@/shared/i18n";

// Chip：完整继承 MUI9 ChipProps，透传全部 props；color 额外允许任意字符串（兼容自由着色）
export interface ChipProps extends Omit<MuiChipProps, "color"> {
  color?: MuiChipProps["color"] | (string & {});
}

export function Chip({ color, ...props }: ChipProps) {
  return <MuiChip color={color as MuiChipProps["color"]} size="small" {...props} />;
}

// Avatar：继承 MUI9 AvatarProps，透传；兼容 name/size 便捷 API
export interface AvatarProps extends MuiAvatarProps {
  /** 兼容：按 name 生成首字母；留空则用 MUI9 children。 */
  name?: string;
  /** 兼容：sm/md/lg → 24/32/40px。 */
  size?: "sm" | "md" | "lg";
}

export function Avatar({ name, src, size, children, ...props }: AvatarProps) {
  const muiSize = size === "sm" ? 24 : size === "lg" ? 40 : size === "md" ? 32 : undefined;
  const initials = name
    ? name
        .split(" ")
        .map((p) => p[0])
        .join("")
        .slice(0, 2)
        .toUpperCase()
    : null;
  return (
    <MuiAvatar src={src} sx={{ width: muiSize, height: muiSize, ...(props.sx as object) }}>
      {children ?? (initials || null)}
    </MuiAvatar>
  );
}

// Tooltip：完整继承 MUI9 TooltipProps，透传全部 props
export interface TooltipProps extends MuiTooltipProps {
  /** 兼容：text → title。 */
  text?: string;
  children: React.ReactElement;
}

export function Tooltip({ text, title, children, ...props }: TooltipProps) {
  return (
    <MuiTooltip title={text ?? title} {...props}>
      {children}
    </MuiTooltip>
  );
}

export function StatusChip({ enabled }: { enabled: boolean }) {
  const { t } = useI18n();
  return (
    <Chip
      label={enabled ? t("common.enabled") : t("common.disabled")}
      color={enabled ? "success" : "default"}
    />
  );
}
