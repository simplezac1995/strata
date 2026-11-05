// SPDX-License-Identifier: Apache-2.0
import React from "react";
import {
  Checkbox as MuiCheckbox,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select as MuiSelect,
  Switch as MuiSwitch,
  TextField as MuiTextField,
} from "@mui/material";
import type {
  CheckboxProps as MuiCheckboxProps,
  SelectProps as MuiSelectProps,
  SwitchProps as MuiSwitchProps,
} from "@mui/material";

type MuiTextFieldProps = React.ComponentProps<typeof MuiTextField>;

// TextField / TextArea：完整继承 MUI9 TextFieldProps，透传全部 props
export interface TextFieldProps extends Omit<MuiTextFieldProps, "error" | "helperText"> {
  /** MUI9 原生 error 布尔，或兼容的错误文本（字符串则显示在 helperText）。 */
  error?: boolean | string;
  /** 兼容别名：辅助说明文本（无 error 时作为 helperText）。 */
  hint?: string;
}

export function TextField({
  label,
  error,
  hint,
  className,
  fullWidth = true,
  ...props
}: TextFieldProps & { fullWidth?: boolean }) {
  const hasErrorText = typeof error === "string";
  const isError = error === true || hasErrorText;
  return (
    <MuiTextField
      label={label}
      error={isError}
      helperText={hasErrorText ? error : (hint ?? undefined)}
      size="small"
      fullWidth={fullWidth}
      className={className}
      {...props}
    />
  );
}

export function TextArea({
  label,
  error,
  hint,
  className,
  rows = 4,
  fullWidth = true,
  ...props
}: TextFieldProps & { rows?: number; fullWidth?: boolean }) {
  const hasErrorText = typeof error === "string";
  const isError = error === true || hasErrorText;
  return (
    <MuiTextField
      label={label}
      error={isError}
      helperText={hasErrorText ? error : (hint ?? undefined)}
      size="small"
      fullWidth={fullWidth}
      multiline
      minRows={rows}
      className={className}
      {...props}
    />
  );
}

// Select：完整继承 MUI9 SelectProps，透传全部 props；onChange 保留原宽松兼容签名
export interface SelectProps extends Omit<MuiSelectProps, "onChange" | "children"> {
  /** 便利 API：由 options 渲染 MenuItem；也可忽略并直接传 MUI9 children。 */
  options?: { value: string; label: string }[];
  /** 透传 children（MUI9 原生 Select 子项）。 */
  children?: React.ReactNode;
  /** 宽松兼容签名：调用点按 HTML select event 书写（e.target.value: string）。 */
  onChange?: React.ChangeEventHandler<HTMLSelectElement>;
}

export function Select({
  label,
  options,
  className,
  value,
  disabled,
  fullWidth = true,
  onChange,
  children,
  ...props
}: SelectProps) {
  const labelId = React.useId();
  return (
    <FormControl
      size="small"
      className={className}
      fullWidth={fullWidth}
      disabled={disabled}
      sx={{ minWidth: 0 }}
    >
      {label ? <InputLabel id={labelId}>{label}</InputLabel> : null}
      <MuiSelect
        labelId={label ? labelId : undefined}
        label={label}
        value={value ?? ""}
        onChange={(e) => onChange?.(e as unknown as React.ChangeEvent<HTMLSelectElement>)}
        {...props}
      >
        {children ??
          options?.map((o) => (
            <MenuItem key={o.value} value={o.value}>
              {o.label}
            </MenuItem>
          ))}
      </MuiSelect>
    </FormControl>
  );
}

// Checkbox：继承 MUI9 CheckboxProps，透传；label 存在时包 FormControlLabel
export function Checkbox({ label, className, ...props }: MuiCheckboxProps & { label?: string }) {
  const control = <MuiCheckbox size="small" {...props} />;
  if (!label) return control;
  return <FormControlLabel className={className} control={control} label={label} />;
}

// Switch：继承 MUI9 SwitchProps，透传；label 存在时包 FormControlLabel
export function Switch({
  checked,
  onChange,
  label,
  ...props
}: Omit<MuiSwitchProps, "checked" | "onChange"> & {
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  label?: string;
}) {
  const control = (
    <MuiSwitch
      checked={checked}
      onChange={(event, value) => {
        onChange?.(value);
      }}
      {...props}
    />
  );
  if (!label) return control;
  return <FormControlLabel control={control} label={label} />;
}
