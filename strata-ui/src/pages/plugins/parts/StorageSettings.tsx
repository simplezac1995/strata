// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Stack, Typography } from "@mui/material";
import { useI18n } from "@/shared/i18n";
import { PageHeader, TextField, Select, Alert } from "@/components/ui/index";
import { pluginApi } from "@/shared/api/demo";
import { toast } from "@/shared/pageKit/Toast";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

export function StorageSettings() {
  const { t } = useI18n();
  const [provider, setProvider] = useState("local");
  const [localActive, setLocalActive] = useState<boolean | null>(null);

  useEffect(() => {
    pluginApi
      .status()
      .then((s) => setLocalActive(s.storageLocal))
      .catch(() => toast.error("toast.loadFailed"));
  }, []);

  const providerFields: Record<string, React.ReactNode> = {
    local: (
      <Stack spacing={2}>
        <Alert type={localActive ? "success" : "warning"}>
          {localActive
            ? t("plugins.storageSettings.localStoragePluginActive")
            : t("plugins.storageSettings.localStoragePluginNotActive")}
        </Alert>
        <TextField
          label={t("plugins.storageSettings.uploadPath")}
          defaultValue="./data/uploads"
          hint={t("plugins.storageSettings.configuredViaStrataStorageLocalBasePath")}
        />
      </Stack>
    ),
    s3: (
      <Stack spacing={2}>
        <Alert type="warning">{t("plugins.storageSettings.s3PluginNotBundled")}</Alert>
        <TextField label={t("plugins.storageSettings.bucketName")} placeholder="my-bucket" />
        <TextField label={t("plugins.storageSettings.region")} placeholder="us-east-1" />
      </Stack>
    ),
    oss: (
      <Stack spacing={2}>
        <Alert type="warning">{t("plugins.storageSettings.ossPluginNotBundled")}</Alert>
        <TextField
          label={t("plugins.storageSettings.endpoint")}
          placeholder="oss-cn-hangzhou.aliyuncs.com"
        />
        <TextField label={t("plugins.storageSettings.bucketName")} placeholder="my-bucket" />
      </Stack>
    ),
    minio: (
      <Stack spacing={2}>
        <Alert type="warning">{t("plugins.storageSettings.minioPluginNotBundled")}</Alert>
        <TextField
          label={t("plugins.storageSettings.endpoint")}
          placeholder="http://localhost:9000"
        />
        <TextField label={t("plugins.storageSettings.bucketName")} placeholder="uploads" />
      </Stack>
    ),
  };

  return (
    <PageStack>
      <PageHeader title={t("plugins.storageSettings.storageSettings")} />
      <Box sx={{ maxWidth: 576 }}>
        <Box sx={{ mb: 2.5 }}>
          <Select
            label={t("system.common.storageProvider")}
            value={provider}
            onChange={(e) => setProvider(e.target.value)}
            options={[
              { value: "local", label: t("plugins.storageSettings.localFilesystem") },
              { value: "s3", label: t("plugins.storageSettings.providerAwsS3") },
              { value: "oss", label: t("plugins.storageSettings.providerAliyunOss") },
              { value: "minio", label: t("plugins.storageSettings.providerMinio") },
            ]}
          />
        </Box>
        <ContentCard sx={{ mb: 2 }}>{providerFields[provider]}</ContentCard>
        <Typography variant="caption" color="text.secondary">
          {t("plugins.config.save.hint")}
        </Typography>
      </Box>
    </PageStack>
  );
}

export default StorageSettings;
