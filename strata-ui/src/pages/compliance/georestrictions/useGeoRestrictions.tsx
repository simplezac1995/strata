// SPDX-License-Identifier: Apache-2.0
// State and handlers of GeoRestrictions, split out of GeoRestrictions.tsx.

import { useState } from "react";
import { Stack, Typography } from "@mui/material";
import { DeleteOutlineOutlined, EditOutlined } from "@mui/icons-material";
import { Button, Chip, Switch } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import {
  complianceApi,
  type GeoCheckIpResult,
  type GeoRegionDto,
  type GeoRegionPayload,
  type GeoRestrictionDto,
  type GeoRestrictionPayload,
} from "@/shared/api/compliance";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import {
  GeoTab,
  RestrictionForm,
  RegionForm,
  ACTION_TONE,
  RESTRICTIONS_KEY,
  REGIONS_KEY,
} from "./part-1";

export function useGeoRestrictions() {
  const { t } = useI18n();

  const [tab, setTab] = useState<GeoTab>("restrictions");

  const restrictions = useListQuery<GeoRestrictionDto>(RESTRICTIONS_KEY, () =>
    complianceApi.geoRestrictionsList(),
  );

  const regions = useListQuery<GeoRegionDto>(REGIONS_KEY, () => complianceApi.geoRegionsList());

  const restrictionsCrud = useResourceCrud<
    GeoRestrictionDto,
    GeoRestrictionPayload,
    GeoRestrictionPayload,
    RestrictionForm
  >({
    create: (payload) => complianceApi.geoRestrictionCreate(payload),
    update: (id, payload) => complianceApi.geoRestrictionUpdate(id, payload),
    remove: (id) => complianceApi.geoRestrictionDelete(id),
    reload: restrictions.reload,
    initForm: () => ({ ipRange: "", country: "", action: "DENY", reason: "", enabled: true }),
    toEditForm: (r) => ({
      ipRange: r.ipRange,
      country: r.country,
      action: r.action,
      reason: r.reason ?? "",
      enabled: r.enabled,
    }),
    toCreatePayload: (form) => ({
      ipRange: form.ipRange,
      country: form.country,
      action: form.action,
      reason: form.reason,
      enabled: form.enabled,
    }),
    toUpdatePayload: (form) => ({
      ipRange: form.ipRange,
      country: form.country,
      action: form.action,
      reason: form.reason,
      enabled: form.enabled,
    }),
  });

  const regionsCrud = useResourceCrud<GeoRegionDto, GeoRegionPayload, unknown, RegionForm>({
    create: (payload) => complianceApi.geoRegionCreate(payload),
    remove: (id) => complianceApi.geoRegionDelete(id),
    reload: regions.reload,
    initForm: () => ({
      countryCode: "",
      countryName: "",
      registerBlocked: false,
      loginBlocked: false,
    }),
    toEditForm: (r) => ({
      countryCode: r.countryCode,
      countryName: r.countryName,
      registerBlocked: r.registerBlocked,
      loginBlocked: r.loginBlocked,
    }),
    toCreatePayload: (form) => ({
      countryCode: form.countryCode,
      countryName: form.countryName,
      registerBlocked: form.registerBlocked,
      loginBlocked: form.loginBlocked,
    }),
  });

  const [ipInput, setIpInput] = useState("");

  const [checking, setChecking] = useState(false);

  const [checkResult, setCheckResult] = useState<GeoCheckIpResult | null>(null);

  const [checkError, setCheckError] = useState("");

  async function handleCheckIp() {
    if (!ipInput.trim()) return;
    setChecking(true);
    setCheckError("");
    setCheckResult(null);
    try {
      const result = await complianceApi.geoCheckIp(ipInput.trim());
      setCheckResult(result);
    } catch (e) {
      setCheckError(e instanceof Error ? e.message : String(e));
    } finally {
      setChecking(false);
    }
  }

  const restrictionColumns = [
    {
      key: "ipRange",
      title: t("compliance.geo.ipRange"),
      render: (r: GeoRestrictionDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.ipRange}
        </Typography>
      ),
    },
    {
      key: "country",
      title: t("compliance.geo.country"),
      render: (r: GeoRestrictionDto) => r.country || "—",
    },
    {
      key: "action",
      title: t("compliance.geo.action"),
      render: (r: GeoRestrictionDto) => (
        <Chip
          label={t(`compliance.geo.action.${r.action}`)}
          color={ACTION_TONE[r.action] ?? "default"}
        />
      ),
    },
    {
      key: "reason",
      title: t("compliance.geo.reason"),
      render: (r: GeoRestrictionDto) => r.reason || "—",
    },
    {
      key: "enabled",
      title: t("common.status"),
      render: (r: GeoRestrictionDto) => (
        <Switch
          checked={r.enabled}
          onChange={(checked) =>
            void restrictionsCrud.updateMutation.mutateAsync({
              id: r.id,
              payload: {
                ipRange: r.ipRange,
                country: r.country,
                action: r.action,
                reason: r.reason ?? "",
                enabled: checked,
              },
            })
          }
          disabled={restrictionsCrud.busy}
        />
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: GeoRestrictionDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button variant="text" size="sm" onClick={() => restrictionsCrud.openEdit(r)}>
            <EditOutlined fontSize="small" />
          </Button>
          <Button
            variant="text"
            size="sm"
            color="danger"
            onClick={() => restrictionsCrud.confirmDelete(r)}
          >
            <DeleteOutlineOutlined fontSize="small" />
          </Button>
        </Stack>
      ),
    },
  ];

  const regionColumns = [
    {
      key: "countryCode",
      title: t("compliance.geo.countryCode"),
      render: (r: GeoRegionDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.countryCode}
        </Typography>
      ),
    },
    {
      key: "countryName",
      title: t("compliance.geo.countryName"),
      render: (r: GeoRegionDto) => r.countryName,
    },
    {
      key: "registerBlocked",
      title: t("compliance.geo.registerBlocked"),
      render: (r: GeoRegionDto) => (
        <Chip
          label={r.registerBlocked ? t("compliance.geo.blocked") : t("compliance.geo.allowed")}
          color={r.registerBlocked ? "error" : "success"}
        />
      ),
    },
    {
      key: "loginBlocked",
      title: t("compliance.geo.loginBlocked"),
      render: (r: GeoRegionDto) => (
        <Chip
          label={r.loginBlocked ? t("compliance.geo.blocked") : t("compliance.geo.allowed")}
          color={r.loginBlocked ? "error" : "success"}
        />
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: GeoRegionDto) => (
        <Button
          variant="text"
          size="sm"
          color="danger"
          onClick={() => regionsCrud.confirmDelete(r)}
        >
          <DeleteOutlineOutlined fontSize="small" />
        </Button>
      ),
    },
  ];

  return {
    t,
    tab,
    setTab,
    restrictions,
    regions,
    restrictionsCrud,
    regionsCrud,
    ipInput,
    setIpInput,
    checking,
    setChecking,
    checkResult,
    setCheckResult,
    checkError,
    setCheckError,
    handleCheckIp,
    restrictionColumns,
    regionColumns,
  };
}
