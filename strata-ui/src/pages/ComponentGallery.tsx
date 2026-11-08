// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Divider, Stack, Typography } from "@mui/material";
import PeopleOutlinedIcon from "@mui/icons-material/PeopleOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import {
  Button,
  TextField,
  Select,
  Checkbox,
  Switch,
  Chip,
  Avatar,
  Dialog,
  ConfirmDialog,
  Alert,
  Card,
  KpiCard,
  Tabs,
  Badge as BadgeComp,
  EmptyState,
  PageHeader,
  QueryForm,
  DataTable,
  Pagination,
  StatusChip,
} from "@/components/ui/index";
import { ProBadge, AiBadge } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { PageStack } from "@/shared/pageKit/PageLayout";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Box sx={{ mb: 6 }}>
      <Typography variant="overline" color="text.secondary" sx={{ display: "block", mb: 2 }}>
        {title}
      </Typography>
      <Divider sx={{ mb: 3 }} />
      {children}
    </Box>
  );
}

const DEMO_TABLE_DATA = [
  { id: 1, name: "Alice Chen", role: "Developer", status: true },
  { id: 2, name: "Bob Kim", role: "Designer", status: true },
  { id: 3, name: "Carol Wu", role: "Finance", status: false },
];

export function ComponentGallery() {
  const { t } = useI18n();
  const [switchOn, setSwitchOn] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [tab, setTab] = useState("overview");
  const [page, setPage] = useState(1);

  return (
    <PageStack sx={{ maxWidth: 960 }}>
      <PageHeader
        title={t("docs.components.components")}
        subtitle={t("docs.components.gallerySubtitle")}
      />

      <Section title={t("docs.components.sectionButton")}>
        <Stack spacing={2}>
          <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
            <Button variant="contained">{t("common.saveChanges")}</Button>
            <Button variant="outlined" color="default">
              {t("common.cancel")}
            </Button>
            <Button variant="text">{t("docs.components.learnMore2")}</Button>
            <Button variant="icon" color="default" aria-label={t("common.search")}>
              <SearchOutlinedIcon />
            </Button>
          </Stack>
          <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
            <Button variant="contained" color="danger">
              {t("common.delete")}
            </Button>
            <Button variant="contained" color="success">
              {t("docs.components.confirm")}
            </Button>
            <Button variant="contained" disabled>
              {t("common.disabled")}
            </Button>
          </Stack>
        </Stack>
      </Section>

      <Section title={t("docs.components.sectionTextField")}>
        <Box
          sx={{
            display: "grid",
            gap: 2,
            gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
            maxWidth: 640,
          }}
        >
          <TextField
            label={t("docs.components.default")}
            placeholder={t("docs.components.enterText")}
          />
          <TextField
            label={t("docs.components.withHint")}
            placeholder={t("common.username")}
            hint={t("docs.components.cannotBeChangedAfterCreation")}
          />
          <TextField
            label={t("docs.components.errorState")}
            placeholder={t("docs.components.enterValue")}
            error={t("docs.components.thisFieldIsRequired")}
          />
          <TextField
            label={t("common.disabled")}
            placeholder={t("docs.components.readonly")}
            disabled
            value="admin"
          />
        </Box>
      </Section>

      <Section title={t("docs.components.sectionSelect")}>
        <Box
          sx={{
            display: "grid",
            gap: 2,
            gridTemplateColumns: { xs: "1fr", md: "repeat(3, 1fr)" },
            maxWidth: 720,
          }}
        >
          <Select
            label={t("common.status")}
            options={[
              { value: "all", label: t("common.all") },
              { value: "yes", label: t("common.enabled") },
              { value: "no", label: t("common.disabled") },
            ]}
          />
          <Select
            label={t("docs.components.role")}
            options={[
              { value: "admin", label: t("shell.admin") },
              { value: "dev", label: t("docs.components.developer") },
              { value: "view", label: t("docs.components.viewer") },
            ]}
          />
          <Select
            label={t("docs.components.pageSize")}
            options={[
              { value: "10", label: t("docs.components.10Page") },
              { value: "20", label: t("docs.components.20Page") },
              { value: "50", label: t("docs.components.50Page") },
            ]}
          />
        </Box>
      </Section>

      <Section title={t("docs.components.sectionControls")}>
        <Stack direction="row" spacing={6} useFlexGap sx={{ flexWrap: "wrap" }}>
          <Stack spacing={1}>
            <Checkbox label={t("docs.components.unchecked")} />
            <Checkbox label={t("docs.components.checked")} defaultChecked />
            <Checkbox label={t("common.disabled")} disabled />
          </Stack>
          <Stack spacing={1}>
            <Switch
              checked={switchOn}
              onChange={setSwitchOn}
              label={switchOn ? t("common.enabled") : t("common.disabled")}
            />
            <Switch checked={false} onChange={() => {}} label={t("docs.components.featureFlag")} />
            <Switch checked onChange={() => {}} label={t("shell.dark.mode")} />
          </Stack>
        </Stack>
      </Section>

      <Section title={t("docs.components.sectionAvatar")}>
        <Stack direction="row" spacing={2} sx={{ alignItems: "flex-end" }}>
          <Avatar name="Alice Chen" size="sm" />
          <Avatar name="Bob Kim" size="md" />
          <Avatar name="Carol Wu" size="lg" />
        </Stack>
      </Section>

      <Section title={t("docs.components.sectionChip")}>
        <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
          <Chip label={t("docs.components.default")} />
          <Chip label={t("docs.components.primary")} color="primary" />
          <Chip label={t("docs.components.success")} color="success" />
          <Chip label={t("docs.components.error")} color="error" />
          <Chip label={t("docs.components.warning")} color="warning" />
          <Chip label={t("docs.components.dismissible")} color="primary" onDelete={() => {}} />
        </Stack>
      </Section>

      <Section title={t("docs.components.sectionBadge")}>
        <Stack
          direction="row"
          spacing={1}
          useFlexGap
          sx={{ flexWrap: "wrap", alignItems: "center" }}
        >
          <BadgeComp variant="success">{t("common.enabled")}</BadgeComp>
          <BadgeComp variant="error">{t("common.disabled")}</BadgeComp>
          <ProBadge />
          <AiBadge />
        </Stack>
      </Section>

      <Section title={t("docs.components.sectionAlert")}>
        <Stack spacing={2} sx={{ maxWidth: 520 }}>
          <Alert type="error">{t("docs.components.invalidUsernameOrPasswordPleaseTryAgain")}</Alert>
          <Alert type="success">{t("docs.components.userCreatedSuccessfully")}</Alert>
          <Alert type="warning">
            {t("docs.components.testModeIsEnabledNoRealPaymentsWillBeProcessed")}
          </Alert>
          <Alert type="info">{t("docs.components.cacheRefreshScheduledForTonightAt0200Utc")}</Alert>
        </Stack>
      </Section>

      <Section title={t("docs.components.sectionTabs")}>
        <Tabs
          tabs={[
            { id: "overview", label: t("nav.overview") },
            { id: "details", label: t("docs.components.details") },
            { id: "logs", label: t("nav.logs") },
          ]}
          active={tab}
          onChange={setTab}
        />
        <Card>
          <Typography variant="body2" color="text.secondary">
            {t("docs.components.activeTab")} <strong>{tab}</strong>
          </Typography>
        </Card>
      </Section>

      <Section title={t("docs.components.sectionDataTable")}>
        <DataTable
          columns={[
            { key: "name", title: t("common.name") },
            { key: "role", title: t("docs.components.role") },
            {
              key: "status",
              title: t("common.status"),
              render: (row) => <StatusChip enabled={row.status} />,
            },
          ]}
          data={DEMO_TABLE_DATA}
          rowKey={(row) => String(row.id)}
        />
        <Pagination total={48} page={page} pageSize={10} onPage={setPage} onPageSize={() => {}} />
      </Section>

      <Section title={t("docs.components.sectionDialog")}>
        <Stack direction="row" spacing={1}>
          <Button variant="contained" onClick={() => setDialogOpen(true)}>
            {t("docs.components.openFormDialog")}
          </Button>
          <Button variant="outlined" color="danger" onClick={() => setConfirmOpen(true)}>
            {t("docs.components.openConfirmDialog")}
          </Button>
        </Stack>
        <Dialog
          open={dialogOpen}
          onClose={() => setDialogOpen(false)}
          title={t("docs.components.configure")}
        >
          <Typography variant="body2">
            {t("docs.components.whiteBackground12pxRadius1pxBorder")}
          </Typography>
        </Dialog>
        <ConfirmDialog
          open={confirmOpen}
          onClose={() => setConfirmOpen(false)}
          onConfirm={() => setConfirmOpen(false)}
          title="system.users.deleteTitle"
          description="docs.components.deleteUserAliceChenThisCannotBeUndone"
        />
      </Section>

      <Section title={t("docs.components.sectionQueryForm")}>
        <QueryForm onSearch={() => {}} onReset={() => {}}>
          <TextField label={t("common.search")} placeholder={t("docs.components.enterText")} />
          <Select
            label={t("common.status")}
            value="all"
            options={[
              { value: "all", label: t("common.all") },
              { value: "yes", label: t("common.enabled") },
            ]}
          />
        </QueryForm>
        <EmptyState
          title={t("common.noData")}
          description={t("docs.components.tryAdjustingYourSearchFiltersOrCreateANewUser")}
        />
      </Section>

      <Section title={t("docs.components.sectionKpiCard")}>
        <Box
          sx={{ display: "grid", gap: 2, gridTemplateColumns: { xs: "1fr", md: "repeat(2, 1fr)" } }}
        >
          <KpiCard
            title={t("docs.components.totalUsers")}
            value="1,284"
            delta={t("docs.components.12ThisWeek")}
            icon={<PeopleOutlinedIcon />}
          />
          <KpiCard
            title={t("docs.components.activeRoles")}
            value="24"
            delta={t("docs.components.3ModifiedToday")}
            icon={<ShieldOutlinedIcon />}
            color="success"
          />
        </Box>
      </Section>
    </PageStack>
  );
}

export default ComponentGallery;
