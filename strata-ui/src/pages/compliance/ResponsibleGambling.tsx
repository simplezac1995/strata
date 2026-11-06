// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Stack, Typography } from "@mui/material";
import { PersonOutlineOutlined } from "@mui/icons-material";
import { Chip, DataTable, PageHeader, QueryForm, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { complianceApi, type ResponsibleUserDto } from "@/shared/api/compliance";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { formatDateTime } from "@/shared/utils/date";

const KEY = ["api", "compliance", "responsible", "users"];

/**
 * Responsible gambling: active user limits (deposit / wager / loss / session).
 * Read-only list filtered by user ID.
 */
export function ResponsibleGambling() {
  const { t } = useI18n();
  const [userId, setUserId] = useState("");

  const { rows, total, page, setPage, pageSize, setPageSize, loading, error, reload, onSearch } =
    usePagedQuery<ResponsibleUserDto>([...KEY, { userId }], (pageIndex, size) =>
      complianceApi.responsibleUsersPage({ userId }, pageIndex, size),
    );

  const columns = [
    {
      key: "userId",
      title: t("compliance.responsible.user"),
      render: (r: ResponsibleUserDto) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
          <PersonOutlineOutlined fontSize="small" color="disabled" />
          <Typography variant="body2">{r.userId || "—"}</Typography>
        </Stack>
      ),
    },
    {
      key: "limitType",
      title: t("compliance.responsible.limitType"),
      render: (r: ResponsibleUserDto) => (
        <Chip label={t(`compliance.responsible.limitType.${r.limitType}`)} color="primary" />
      ),
    },
    {
      key: "amount",
      title: t("compliance.responsible.amount"),
      render: (r: ResponsibleUserDto) => (
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {r.amount}
        </Typography>
      ),
    },
    {
      key: "period",
      title: t("compliance.responsible.period"),
      render: (r: ResponsibleUserDto) => r.period,
    },
    {
      key: "startTime",
      title: t("compliance.responsible.startTime"),
      render: (r: ResponsibleUserDto) => (
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
          {formatDateTime(r.startTime)}
        </Typography>
      ),
    },
    {
      key: "endTime",
      title: t("compliance.responsible.endTime"),
      render: (r: ResponsibleUserDto) => (
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
          {r.endTime ? formatDateTime(r.endTime) : t("common.none")}
        </Typography>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("compliance.responsible.title")}
        subtitle={t("compliance.responsible.subtitle")}
      />

      <QueryForm
        onSearch={onSearch}
        onReset={() => {
          setUserId("");
          setPage(1);
        }}
      >
        <TextField
          label={t("compliance.responsible.user")}
          placeholder={t("common.searchPlaceholder")}
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
        />
      </QueryForm>

      <DataTable
        columns={columns}
        data={rows}
        loading={loading}
        rowKey={(r) => r.id}
        total={total}
        page={page}
        pageSize={pageSize}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        tableId="compliance.responsible.users"
        error={error}
        onRetry={reload}
      />
    </PageStack>
  );
}

export default ResponsibleGambling;
