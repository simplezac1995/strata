// SPDX-License-Identifier: Apache-2.0
import { useMemo } from "react";
import { FormControl, InputLabel, MenuItem, Select, type SelectProps } from "@mui/material";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { modelApi } from "@/shared/api/ai";
import type { AiModelDto } from "@/shared/types";
import { useI18n } from "@/shared/i18n";

export interface ModelSelectorProps extends Omit<SelectProps<string>, "children"> {
  label?: string;
  providerId?: string;
  modelType?: string;
}

/**
 * AI 模型选择器
 * 自动加载模型列表，支持按提供商和类型过滤
 */
export function ModelSelector({
  label = "ai.config.model",
  providerId,
  modelType,
  ...selectProps
}: ModelSelectorProps) {
  const { t } = useI18n();
  const { data: models, loading } = useListQuery<AiModelDto>(
    ["api", "ai", "models", "list", providerId, modelType],
    modelApi.list,
    { staleTime: 5 * 60 * 1000 },
  );

  const filteredModels = useMemo(() => {
    if (!models) return [];
    return models.filter((m) => m.enabled !== false);
  }, [models]);

  return (
    <FormControl fullWidth size="small">
      <InputLabel>{label}</InputLabel>
      <Select label={label} {...selectProps}>
        {loading && <MenuItem disabled>{t("common.loading")}</MenuItem>}
        {filteredModels.map((model) => (
          <MenuItem key={model.id} value={model.id}>
            {model.name} ({model.code})
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
