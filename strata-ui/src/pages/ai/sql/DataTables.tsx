// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import TableChartIcon from "@mui/icons-material/TableChart";
import SearchIcon from "@mui/icons-material/Search";
import SyncIcon from "@mui/icons-material/Sync";
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import KeyIcon from "@mui/icons-material/Key";
import {
  Box,
  Chip,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import { Button, Select, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { apiGet } from "@/shared/api/client";

interface TableInfo {
  tableName: string;
  tableComment: string;
  tableRows: number;
}

interface ColumnInfo {
  columnName: string;
  columnType: string;
  columnComment: string;
  columnKey: string;
  isNullable: string;
  columnDefault: string;
}

export function DataTables() {
  const [tables, setTables] = useState<TableInfo[]>([]);
  const [columns, setColumns] = useState<ColumnInfo[]>([]);
  const [selected, setSelected] = useState<string>("");
  const [keyword, setKeyword] = useState("");

  useEffect(() => {
    apiGet<TableInfo[]>("/api/v1/ai/sql/tables")
      .then((res) => {
        setTables(res);
        if (res.length > 0) setSelected(res[0].tableName);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!selected) return;
    apiGet<ColumnInfo[]>(`/api/v1/ai/sql/columns?table=${selected}`)
      .then(setColumns)
      .catch(() => setColumns([]));
  }, [selected]);

  const filtered = tables.filter(
    (t) => !keyword || t.tableName.toLowerCase().includes(keyword.toLowerCase()),
  );

  const selectedTable = tables.find((t) => t.tableName === selected);

  return (
    <PageStack>
      <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 180px)", minHeight: 420 }}>
        {/* Left: datasource + table list */}
        <Paper variant="outlined" sx={{ width: 300, p: 2, overflowY: "auto", flexShrink: 0 }}>
          <Box
            sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 1.5 }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
              <TableChartIcon fontSize="small" />
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                数据表管理
              </Typography>
            </Box>
            <Select
              size="small"
              value=""
              onChange={() => {}}
              options={[{ value: "", label: "选择数据源" }]}
              sx={{ width: 120 }}
            />
          </Box>
          <TextField
            placeholder="搜索名称"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            slotProps={{
              input: { startAdornment: <SearchIcon sx={{ mr: 1, color: "text.disabled" }} /> },
            }}
            sx={{ mb: 1.5 }}
          />
          <Box sx={{ display: "flex", gap: 1, mb: 1.5 }}>
            <Button size="small" variant="outlined" color="default" startIcon={<SyncIcon />}>
              同步表和视图
            </Button>
            <Button
              size="small"
              variant="outlined"
              color="default"
              startIcon={<DeleteOutlinedIcon />}
            >
              删除选中项
            </Button>
          </Box>
          <Box>
            {filtered.map((t) => (
              <Box
                key={t.tableName}
                onClick={() => setSelected(t.tableName)}
                sx={{
                  px: 1.5,
                  py: 1,
                  cursor: "pointer",
                  borderRadius: 1,
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  bgcolor: selected === t.tableName ? "action.selected" : "transparent",
                  "&:hover": { bgcolor: "action.hover" },
                }}
              >
                <TableChartIcon sx={{ fontSize: 16, color: "text.secondary" }} />
                <Box sx={{ minWidth: 0 }}>
                  <Typography
                    variant="body2"
                    sx={{ fontWeight: selected === t.tableName ? 600 : 400 }}
                    noWrap
                  >
                    {t.tableName}
                  </Typography>
                </Box>
              </Box>
            ))}
            {filtered.length === 0 && (
              <Typography
                variant="body2"
                sx={{ color: "text.secondary", textAlign: "center", py: 3 }}
              >
                暂无数据表
              </Typography>
            )}
          </Box>
        </Paper>

        {/* Right: selected table field detail */}
        <Paper variant="outlined" sx={{ flex: 1, p: 2.5, overflowY: "auto" }}>
          <Box
            sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2 }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <TableChartIcon />
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {selected || "请选择数据表"}
              </Typography>
              <Chip label="物理表" size="small" variant="outlined" />
            </Box>
            <Box sx={{ display: "flex", gap: 1 }}>
              <Button size="small" variant="outlined" color="default" startIcon={<SyncIcon />}>
                同步字段
              </Button>
              <Button size="small" variant="outlined" color="default">
                评估字段
              </Button>
            </Box>
          </Box>

          <Box sx={{ display: "flex", gap: 2, mb: 1.5, alignItems: "center" }}>
            <Typography sx={{ width: 72, color: "text.secondary" }}>物理注释</Typography>
            <Typography variant="body2">{selectedTable?.tableComment || "暂无"}</Typography>
          </Box>
          <Box sx={{ display: "flex", gap: 2, mb: 2.5, alignItems: "center" }}>
            <Typography sx={{ width: 72, color: "text.secondary" }}>逻辑注释</Typography>
            <TextField placeholder="请输入业务含义、每行粒度和统计口径" sx={{ flex: 1 }} />
          </Box>

          <Box
            sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 1 }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <TableChartIcon fontSize="small" />
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                字段列表
              </Typography>
              <Chip label={columns.length} size="small" color="primary" />
            </Box>
            <Typography variant="body2" sx={{ color: "text.secondary" }}>
              共 {columns.length} 个字段
            </Typography>
          </Box>

          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "action.hover" }}>
                <TableCell sx={{ fontWeight: 600 }}>主键</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>字段名称</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>字段类型</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>字段备注</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>虚拟备注</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>修正状态</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {columns.map((c) => (
                <TableRow key={c.columnName} hover>
                  <TableCell>
                    {c.columnKey === "PRI" ? <KeyIcon sx={{ fontSize: 16 }} /> : "-"}
                  </TableCell>
                  <TableCell sx={{ fontFamily: "monospace", fontSize: 13 }}>
                    {c.columnName}
                  </TableCell>
                  <TableCell>
                    <Chip label={c.columnType} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell>{c.columnComment || "-"}</TableCell>
                  <TableCell sx={{ minWidth: 220 }}>
                    <TextField
                      placeholder="请输入虚拟备注"
                      sx={{ "& .MuiOutlinedInput-root": { fontSize: 13 } }}
                    />
                  </TableCell>
                  <TableCell>{c.columnComment ? "是" : "否"}</TableCell>
                </TableRow>
              ))}
              {columns.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} sx={{ textAlign: "center", color: "text.secondary" }}>
                    暂无数据
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default DataTables;
