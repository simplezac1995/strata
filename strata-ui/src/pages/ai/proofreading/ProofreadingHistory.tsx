// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Stack, Typography } from "@mui/material";
import {
  AiPageHeader,
  Button,
  DataTable,
  KpiCard,
  Pagination,
  Select,
  TextField,
} from "@/components/ui/index";
import { PageStack, PageGrid } from "@/shared/pageKit/PageLayout";
import { aiToolApi, type AiUsageDto } from "@/shared/api/ai";
import {
  CheckCircle,
  Description,
  Error as ErrorIcon,
  Pending,
  Refresh,
  Search,
  Visibility,
} from "@mui/icons-material";

interface HistoryRow {
  id: string;
  taskId: string;
  fileName: string;
  status: string;
  creator: string;
  createdAt: string;
  finishedAt: string;
  updatedAt: string;
  error: string;
}

const STATUS_OPTIONS = [
  { value: "", label: "请选择状态" },
  { value: "done", label: "已完成" },
  { value: "running", label: "进行中" },
  { value: "error", label: "错误" },
];

export function ProofreadingHistory() {
  const [usage, setUsage] = useState<AiUsageDto | null>(null);
  const [fileName, setFileName] = useState("");
  const [status, setStatus] = useState("");
  const [rows] = useState<HistoryRow[]>([]);

  useEffect(() => {
    aiToolApi
      .usage()
      .then(setUsage)
      .catch(() => {});
  }, []);

  const cols = [
    { key: "index", title: "序号", render: (r: HistoryRow) => r.id },
    {
      key: "taskId",
      title: "任务ID",
      render: (r: HistoryRow) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace" }}>
          {r.taskId}
        </Box>
      ),
    },
    { key: "fileName", title: "文件名", align: "left" as const },
    {
      key: "status",
      title: "状态",
      render: (r: HistoryRow) => (
        <Box component="span" sx={{ color: "primary.main", fontSize: 13 }}>
          {r.status}
        </Box>
      ),
    },
    { key: "creator", title: "创建人" },
    { key: "createdAt", title: "创建时间" },
    { key: "finishedAt", title: "完成时间" },
    { key: "updatedAt", title: "更新时间" },
    { key: "error", title: "错误信息" },
    {
      key: "actions",
      title: "操作",
      render: () => (
        <Stack direction="row" spacing={0.5}>
          <Button
            variant="text"
            size="sm"
            startIcon={<Visibility sx={{ fontSize: 14 }} />}
            disabled
          >
            查看
          </Button>
          <Button variant="text" size="sm" color="danger" disabled>
            删除
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <AiPageHeader title="审校历史" subtitle="查看、检索和管理所有审校任务记录" />

      <PageGrid>
        <KpiCard
          title="累计任务"
          value={usage?.documentCount ?? 0}
          icon={<Description />}
          color="primary"
        />
        <KpiCard
          title="已完成"
          value={usage?.messageCount ?? 0}
          icon={<CheckCircle />}
          color="success"
        />
        <KpiCard title="任务错误" value={0} icon={<ErrorIcon />} color="error" />
        <KpiCard title="进行中" value={0} icon={<Pending />} color="warning" />
      </PageGrid>

      {/* Filter bar */}
      <Box
        sx={{
          display: "flex",
          gap: 1.5,
          flexWrap: "wrap",
          alignItems: "center",
          "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
        }}
      >
        <Typography variant="body2" sx={{ color: "text.secondary" }}>
          文件名
        </Typography>
        <TextField
          size="small"
          placeholder="请输入文件名"
          value={fileName}
          onChange={(e) => setFileName(e.target.value)}
          sx={{ width: 220 }}
        />
        <Typography variant="body2" sx={{ color: "text.secondary", ml: 1 }}>
          任务状态
        </Typography>
        <Select
          size="small"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          options={STATUS_OPTIONS}
          sx={{ width: 160 }}
        />
        <Button variant="contained" size="small" startIcon={<Search sx={{ fontSize: 14 }} />}>
          查询
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<Refresh sx={{ fontSize: 14 }} />}
          onClick={() => {
            setFileName("");
            setStatus("");
          }}
        >
          重置
        </Button>
      </Box>

      <DataTable columns={cols} data={rows} rowKey={(r) => r.id} />
      <Pagination
        page={1}
        pageSize={10}
        total={rows.length}
        onPage={() => {}}
        onPageSize={() => {}}
      />
    </PageStack>
  );
}

export default ProofreadingHistory;
