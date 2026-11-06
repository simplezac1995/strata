// SPDX-License-Identifier: Apache-2.0
import { useMemo } from "react";
import { FormControl, InputLabel, MenuItem, Select, type SelectProps } from "@mui/material";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { providerApi } from "@/shared/api/ai";
import type { AiProviderDto } from "@/shared/types";
import { useI18n } from "@/shared/i18n";

export interface ProviderSelectorProps extends Omit<SelectProps<string>, "children"> {
  label?: string;
  providerType?: string;
}

/**
 * AI 提供商选择器
 * 自动加载提供商列表，支持按类型过滤
 */
export function ProviderSelector({
  label = "ai.config.provider",
  providerType,
  ...selectProps
}: ProviderSelectorProps) {
  const { t } = useI18n();
  const { data: providers, loading } = useListQuery<AiProviderDto>(
    ["api", "ai", "providers", "list", providerType],
    providerApi.list,
    { staleTime: 5 * 60 * 1000 },
  );

  const filteredProviders = useMemo(() => {
    if (!providers) return [];
    return providers.filter((p) => p.enabled !== false);
  }, [providers]);

  return (
    <FormControl fullWidth size="small">
      <InputLabel>{label}</InputLabel>
      <Select label={label} {...selectProps}>
        {loading && <MenuItem disabled>{t("common.loading")}</MenuItem>}
        {filteredProviders.map((provider) => (
          <MenuItem key={provider.id} value={provider.id}>
            {provider.name} ({provider.code})
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
