// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { IconButton, Button as MuiButton } from "@mui/material";
import type { ButtonProps as MuiButtonProps } from "@mui/material/Button";
import type { IconButtonProps } from "@mui/material/IconButton";

// 兼容别名 → MUI9 原生值
type AliasColor = "danger" | "default";
type AliasSize = "sm" | "lg";

// color: MUI9 原生值为主；危险色/默认色保留兼容别名
type ButtonColor = MuiButtonProps["color"] | AliasColor;
type ButtonSize = MuiButtonProps["size"] | AliasSize;

interface ButtonProps extends Omit<MuiButtonProps, "color" | "size" | "variant"> {
  color?: ButtonColor;
  size?: ButtonSize;
  variant?: MuiButtonProps["variant"] | "icon";
  children: ReactNode;
}

function resolveColor(color: NonNullable<ButtonColor>): MuiButtonProps["color"] {
  if (color === "danger") return "error";
  if (color === "default") return "inherit";
  return color;
}

function resolveSize(size: NonNullable<ButtonSize>): MuiButtonProps["size"] {
  if (size === "sm") return "small";
  if (size === "lg") return "large";
  return size;
}

export function Button({
  variant = "contained",
  size,
  color,
  children,
  className,
  ...props
}: ButtonProps) {
  const muiColor = color ? resolveColor(color) : "primary";
  const muiSize = size ? resolveSize(size) : "medium";

  if (variant === "icon") {
    const iconProps: IconButtonProps = {
      color: muiColor,
      size: muiSize,
      className,
      ...props,
    };
    return <IconButton {...iconProps}>{children}</IconButton>;
  }

  return (
    <MuiButton variant={variant} color={muiColor} size={muiSize} className={className} {...props}>
      {children}
    </MuiButton>
  );
}
