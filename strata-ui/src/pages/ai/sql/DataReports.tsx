// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import QueryStatsIcon from "@mui/icons-material/QueryStats";
import SearchIcon from "@mui/icons-material/Search";
import RefreshIcon from "@mui/icons-material/Refresh";
import DeleteIcon from "@mui/icons-material/DeleteOutlined";
import InboxIcon from "@mui/icons-material/Inventory2";
import { Box, IconButton, Typography } from "@mui/material";
import { Button, Select, TextField, EmptyState, Pagination } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { datasetApi } from "@/shared/api/ai";
import { useListQuery } from "@/shared/hooks/useListQuery";

export function DataReports() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [dataset, setDataset] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Real dataset options from the existing datasets endpoint.
  const { data: datasets } = useListQuery(["api", "ai", "datasets", "list"], datasetApi.list);

  const goToChat = () => navigate("/ai/sql");

  return (
    <PageStack>
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
        <Typography variant="body2">报告标题</Typography>
        <TextField
          size="small"
          placeholder="请输入报告标题"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          sx={{ width: 220 }}
        />
        <Typography variant="body2">数据集</Typography>
        <Select
          size="small"
          value={dataset}
          onChange={(e) => setDataset(e.target.value)}
          sx={{ width: 180 }}
          options={[
            { value: "", label: "全部数据集" },
            ...datasets.map((d) => ({ value: d.id, label: d.name })),
          ]}
        />
        <Button variant="contained" size="small" startIcon={<SearchIcon />}>
          查询
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<RefreshIcon />}
          onClick={() => {
            setTitle("");
            setDataset("");
          }}
        >
          重置
        </Button>
      </Box>

      {/* Toolbar */}
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Box sx={{ display: "flex", gap: 1 }}>
          <Button variant="contained" startIcon={<QueryStatsIcon />} onClick={goToChat}>
            前往 AI 问数
          </Button>
          <Button variant="outlined" color="default" startIcon={<DeleteIcon />}>
            删除
          </Button>
        </Box>
        <Box sx={{ display: "flex", gap: 1 }}>
          {[SearchIcon, RefreshIcon].map((Icon, i) => (
            <IconButton
              key={i}
              sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
            >
              <Icon fontSize="small" />
            </IconButton>
          ))}
        </Box>
      </Box>

      {/* Empty state */}
      <EmptyState
        icon={<InboxIcon sx={{ fontSize: 40 }} />}
        title="暂无数据报告"
        description="请前往 AI 问数勾选数据结果后生成报告"
        action={
          <Button variant="contained" onClick={goToChat}>
            前往 AI 问数
          </Button>
        }
      />

      <Pagination
        page={page}
        pageSize={pageSize}
        total={0}
        onPage={setPage}
        onPageSize={(size: number) => {
          setPageSize(size);
          setPage(1);
        }}
      />
    </PageStack>
  );
}

export default DataReports;
