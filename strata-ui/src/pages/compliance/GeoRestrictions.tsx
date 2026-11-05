// SPDX-License-Identifier: Apache-2.0
import { Box, DialogActions, Stack } from "@mui/material";
import { AddCircleOutlineOutlined, TravelExploreOutlined } from "@mui/icons-material";
import {
  Alert,
  Button,
  ConfirmDialog,
  DataTable,
  Dialog,
  PageHeader,
  Select,
  Switch,
  Tabs,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { type GeoRestrictionAction } from "@/shared/api/compliance";
import { GeoTab } from "./georestrictions/part-1";
import { useGeoRestrictions } from "./georestrictions/useGeoRestrictions";

export function GeoRestrictions() {
  const {
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
  } = useGeoRestrictions();

  return (
    <PageStack>
      <PageHeader title={t("compliance.geo.title")} subtitle={t("compliance.geo.subtitle")} />

      {/* Test IP bar */}
      <Box
        component="div"
        sx={{
          display: "flex",
          gap: 1.5,
          alignItems: "center",
          flexWrap: "wrap",
        }}
      >
        <TextField
          label={t("compliance.geo.testIp")}
          placeholder="1.2.3.4"
          value={ipInput}
          onChange={(e) => setIpInput(e.target.value)}
          sx={{ minWidth: 260 }}
        />
        <Button
          variant="contained"
          disabled={!ipInput.trim() || checking}
          onClick={() => void handleCheckIp()}
        >
          <TravelExploreOutlined fontSize="small" sx={{ mr: 0.5 }} />
          {checking ? t("common.loading") : t("compliance.geo.check")}
        </Button>
        {checkResult ? (
          <Alert type={checkResult.blocked ? "warning" : "success"} sx={{ flex: 1, minWidth: 280 }}>
            {checkResult.blocked
              ? t("compliance.geo.blockedResult").replace("{ip}", checkResult.ip)
              : t("compliance.geo.allowedResult").replace("{ip}", checkResult.ip)}
            {checkResult.country ? ` (${checkResult.country})` : ""}
            {checkResult.matchedRule ? ` — ${checkResult.matchedRule}` : ""}
          </Alert>
        ) : null}
        {checkError ? <Alert type="error">{checkError}</Alert> : null}
      </Box>

      <Tabs
        tabs={[
          { id: "restrictions", label: t("compliance.geo.tab.restrictions") },
          { id: "regions", label: t("compliance.geo.tab.regions") },
        ]}
        active={tab}
        onChange={(id) => setTab(id as GeoTab)}
      />

      {tab === "restrictions" ? (
        <>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
            <Box component="div" sx={{ flex: 1 }} />
            <Button variant="contained" onClick={restrictionsCrud.openCreate}>
              <AddCircleOutlineOutlined fontSize="small" sx={{ mr: 0.5 }} />
              {t("compliance.geo.restrictionCreate")}
            </Button>
          </Stack>
          <DataTable
            columns={restrictionColumns}
            data={restrictions.data}
            loading={restrictions.loading}
            rowKey={(r) => r.id}
            tableId="compliance.geo.restrictions"
            onRetry={restrictions.reload}
          />
        </>
      ) : (
        <>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
            <Box component="div" sx={{ flex: 1 }} />
            <Button variant="contained" onClick={regionsCrud.openCreate}>
              <AddCircleOutlineOutlined fontSize="small" sx={{ mr: 0.5 }} />
              {t("compliance.geo.regionCreate")}
            </Button>
          </Stack>
          <DataTable
            columns={regionColumns}
            data={regions.data}
            loading={regions.loading}
            rowKey={(r) => r.id}
            tableId="compliance.geo.regions"
            onRetry={regions.reload}
          />
        </>
      )}

      {/* Restriction create/edit dialog */}
      <Dialog
        open={restrictionsCrud.open}
        onClose={restrictionsCrud.closeDialog}
        title={
          restrictionsCrud.edit
            ? t("compliance.geo.restrictionEdit")
            : t("compliance.geo.restrictionCreate")
        }
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("compliance.geo.ipRange")}
            placeholder="1.2.3.0/24"
            value={restrictionsCrud.form.ipRange}
            onChange={(e) => restrictionsCrud.setForm((f) => ({ ...f, ipRange: e.target.value }))}
          />
          <TextField
            label={t("compliance.geo.country")}
            value={restrictionsCrud.form.country}
            onChange={(e) => restrictionsCrud.setForm((f) => ({ ...f, country: e.target.value }))}
          />
          <Select
            label={t("compliance.geo.action")}
            value={restrictionsCrud.form.action}
            onChange={(e) =>
              restrictionsCrud.setForm((f) => ({
                ...f,
                action: e.target.value as GeoRestrictionAction,
              }))
            }
            options={[
              { value: "ALLOW", label: t("compliance.geo.action.ALLOW") },
              { value: "DENY", label: t("compliance.geo.action.DENY") },
            ]}
          />
          <TextField
            label={t("compliance.geo.reason")}
            value={restrictionsCrud.form.reason}
            onChange={(e) => restrictionsCrud.setForm((f) => ({ ...f, reason: e.target.value }))}
          />
          <Switch
            checked={restrictionsCrud.form.enabled}
            onChange={(checked) => restrictionsCrud.setForm((f) => ({ ...f, enabled: checked }))}
            label={t("common.enabled")}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={restrictionsCrud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={restrictionsCrud.busy || !restrictionsCrud.form.ipRange}
            onClick={() => void restrictionsCrud.handleSave()}
          >
            {restrictionsCrud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Region create dialog */}
      <Dialog
        open={regionsCrud.open}
        onClose={regionsCrud.closeDialog}
        title={t("compliance.geo.regionCreate")}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label={t("compliance.geo.countryCode")}
            placeholder="CN"
            value={regionsCrud.form.countryCode}
            onChange={(e) => regionsCrud.setForm((f) => ({ ...f, countryCode: e.target.value }))}
          />
          <TextField
            label={t("compliance.geo.countryName")}
            value={regionsCrud.form.countryName}
            onChange={(e) => regionsCrud.setForm((f) => ({ ...f, countryName: e.target.value }))}
          />
          <Switch
            checked={regionsCrud.form.registerBlocked}
            onChange={(checked) => regionsCrud.setForm((f) => ({ ...f, registerBlocked: checked }))}
            label={t("compliance.geo.registerBlocked")}
          />
          <Switch
            checked={regionsCrud.form.loginBlocked}
            onChange={(checked) => regionsCrud.setForm((f) => ({ ...f, loginBlocked: checked }))}
            label={t("compliance.geo.loginBlocked")}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={regionsCrud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            disabled={
              regionsCrud.busy || !regionsCrud.form.countryCode || !regionsCrud.form.countryName
            }
            onClick={() => void regionsCrud.handleSave()}
          >
            {regionsCrud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete confirmations */}
      <ConfirmDialog
        open={restrictionsCrud.deleting !== null}
        onClose={restrictionsCrud.closeDelete}
        onConfirm={() => void restrictionsCrud.runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace(
          "{name}",
          restrictionsCrud.deleting?.ipRange ?? "",
        )}
        confirmColor="danger"
      />
      <ConfirmDialog
        open={regionsCrud.deleting !== null}
        onClose={regionsCrud.closeDelete}
        onConfirm={() => void regionsCrud.runDelete()}
        title={t("common.delete")}
        description={t("common.deleteNamed").replace(
          "{name}",
          regionsCrud.deleting?.countryCode ?? "",
        )}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default GeoRestrictions;
