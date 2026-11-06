// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Stack,
  Typography,
  Chip,
  Radio,
  FormControlLabel,
} from "@mui/material";
import { Description, RestartAlt, DeleteOutlined, CheckCircle } from "@mui/icons-material";
import {
  Button,
  TextField,
  Select,
  ConfirmDialog,
  Pagination,
  QueryForm,
  QueryField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { AiPageHeader } from "@/components/ui/index";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { knowledgeApi } from "@/shared/api/ai";
import { apiGet } from "@/shared/api/client";
import { toast } from "@/shared/pageKit/Toast";
import type { AiKnowledgeDto, PageResult } from "@/shared/types";

interface ChunkRow {
  id: number;
  documentId: number;
  chunkIndex: number;
  content: string;
  contentLength: number;
}

export function Chunks() {
  const { data: kbs } = useListQuery<AiKnowledgeDto>(
    ["api", "ai", "knowledge-bases", "list"],
    knowledgeApi.list,
    { staleTime: 5 * 60 * 1000 },
  );

  // 草稿筛选
  const [kbFilter, setKbFilter] = useState("");
  const [nameFilter, setNameFilter] = useState("");
  const [contentFilter, setContentFilter] = useState("");
  const [trained, setTrained] = useState("");
  // 已应用
  const [appliedContent, setAppliedContent] = useState("");

  const {
    rows: allRows,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
  } = usePagedQuery<ChunkRow>(
    ["api", "ai", "chunks"],
    (pageIdx, size) =>
      apiGet<PageResult<ChunkRow>>(`/api/v1/ai/chunks?page=${pageIdx}&size=${size}`),
    { initialPageSize: 6 },
  );

  const [retrainRow, setRetrainRow] = useState<ChunkRow | null>(null);
  const [deleteRow, setDeleteRow] = useState<ChunkRow | null>(null);

  const rows = useMemo(
    () =>
      allRows.filter(
        (r) => !appliedContent || r.content.toLowerCase().includes(appliedContent.toLowerCase()),
      ),
    [allRows, appliedContent],
  );

  return (
    <PageStack>
      <AiPageHeader title="切片管理" subtitle="知识库文档切片浏览与重训" />

      <QueryForm
        onSearch={() => {
          setAppliedContent(contentFilter);
          setPage(1);
        }}
        onReset={() => {
          setKbFilter("");
          setNameFilter("");
          setContentFilter("");
          setTrained("");
          setAppliedContent("");
          setPage(1);
        }}
      >
        <QueryField width={200}>
          <Select
            label="知识库名"
            value={kbFilter}
            onChange={(e) => setKbFilter(e.target.value)}
            options={[
              { value: "", label: "全部" },
              ...kbs.map((k) => ({ value: k.id, label: k.name })),
            ]}
          />
        </QueryField>
        <QueryField width={180}>
          <TextField
            label="文件名"
            value={nameFilter}
            onChange={(e) => setNameFilter(e.target.value)}
            placeholder="请输入文件名"
          />
        </QueryField>
        <QueryField width={200}>
          <TextField
            label="内容"
            value={contentFilter}
            onChange={(e) => setContentFilter(e.target.value)}
            placeholder="请输入切片内容"
          />
        </QueryField>
        <QueryField width={180}>
          <Stack direction="row" sx={{ alignItems: "center", height: "100%" }}>
            <Typography variant="body2" sx={{ color: "text.secondary", mr: 1 }}>
              已训练
            </Typography>
            <FormControlLabel
              control={
                <Radio size="small" checked={trained === "no"} onChange={() => setTrained("no")} />
              }
              label="否"
            />
            <FormControlLabel
              control={
                <Radio
                  size="small"
                  checked={trained === "yes"}
                  onChange={() => setTrained("yes")}
                />
              }
              label="是"
            />
          </Stack>
        </QueryField>
      </QueryForm>

      {/* 两列切片卡片 */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
          gap: 2,
        }}
      >
        {(loading ? [] : rows).map((r) => (
          <Card key={r.id} variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
                <Description sx={{ color: "primary.main" }} />
                <Typography
                  variant="subtitle2"
                  sx={{ fontWeight: 600, flex: 1, minWidth: 0 }}
                  noWrap
                >
                  切片 #{r.chunkIndex}
                </Typography>
                <Chip
                  icon={<CheckCircle sx={{ fontSize: 14 }} />}
                  label="已训练"
                  size="small"
                  color="success"
                  variant="outlined"
                />
              </Box>
              <Typography
                variant="body2"
                sx={{
                  color: "text.secondary",
                  display: "-webkit-box",
                  WebkitLineClamp: 4,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                  minHeight: 60,
                }}
              >
                {r.content}
              </Typography>
              <Stack
                direction="row"
                sx={{
                  justifyContent: "space-between",
                  alignItems: "center",
                  mt: 1.5,
                  pt: 1.5,
                  borderTop: 1,
                  borderColor: "divider",
                }}
              >
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  {r.contentLength} 字符 · 命中 - 次
                </Typography>
                <Stack direction="row" spacing={1}>
                  <Button
                    variant="text"
                    size="sm"
                    startIcon={<RestartAlt sx={{ fontSize: 15 }} />}
                    onClick={() => setRetrainRow(r)}
                  >
                    重新训练
                  </Button>
                  <Button
                    variant="text"
                    size="sm"
                    color="danger"
                    startIcon={<DeleteOutlined sx={{ fontSize: 15 }} />}
                    onClick={() => setDeleteRow(r)}
                  >
                    删除
                  </Button>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Box>

      <Pagination
        page={page}
        pageSize={pageSize}
        total={total}
        onPage={setPage}
        onPageSize={(size: number) => {
          setPageSize(size);
          setPage(1);
        }}
      />

      <ConfirmDialog
        open={retrainRow !== null}
        onClose={() => setRetrainRow(null)}
        onConfirm={() => {
          setRetrainRow(null);
          reload();
        }}
        title="系统提示"
        description="确认要重新训练该切片吗？"
      />

      <ConfirmDialog
        open={deleteRow !== null}
        onClose={() => setDeleteRow(null)}
        onConfirm={() => {
          setDeleteRow(null);
          toast.info("删除切片接口暂未接入");
        }}
        title="删除切片"
        description="确认删除该切片？此操作不可恢复。"
      />
    </PageStack>
  );
}

export default Chunks;
