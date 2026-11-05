// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { Alert as MuiAlert } from "@mui/material";
import type { AlertProps as MuiAlertProps } from "@mui/material";

// 兼容别名：type → MUI9 severity
export interface AlertProps extends Omit<MuiAlertProps, "severity"> {
  /** MUI9 severity 值；兼容旧别名 type。 */
  type?: MuiAlertProps["severity"];
  children: ReactNode;
}

export function Alert({ type = "error", children, ...props }: AlertProps) {
  return (
    <MuiAlert severity={type} variant="standard" {...props}>
      {children}
    </MuiAlert>
  );
}
