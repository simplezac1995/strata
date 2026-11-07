// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { IconButton, InputAdornment, TextField, type TextFieldProps } from "@mui/material";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";
import { useI18n } from "@/shared/i18n";

export interface ApiKeyFieldProps extends Omit<TextFieldProps, "type"> {
  /** 是否默认显示明文 */
  defaultVisible?: boolean;
}

/**
 * API Key 输入字段
 * 支持密码模式和明文显示切换，自动添加 autocomplete="off"
 */
export function ApiKeyField({ defaultVisible = false, ...textFieldProps }: ApiKeyFieldProps) {
  const [visible, setVisible] = useState(defaultVisible);
  const { t } = useI18n();

  return (
    <TextField
      {...textFieldProps}
      type={visible ? "text" : "password"}
      autoComplete="off"
      slotProps={{
        input: {
          endAdornment: (
            <InputAdornment position="end">
              <IconButton
                aria-label={visible ? t("common.hideApiKey") : t("common.showApiKey")}
                onClick={() => setVisible((v) => !v)}
                edge="end"
                size="small"
              >
                {visible ? <VisibilityOff fontSize="small" /> : <Visibility fontSize="small" />}
              </IconButton>
            </InputAdornment>
          ),
        },
      }}
    />
  );
}
