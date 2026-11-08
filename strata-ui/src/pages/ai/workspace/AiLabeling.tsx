// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Chip, Stack, Typography } from "@mui/material";
import LightbulbIcon from "@mui/icons-material/Lightbulb";
import { Button, DataTable, Switch, TextField, Select } from "@/components/ui/index";
import { AiPageHeader, StatusChip } from "@/components/ui/index";
import { datasetApi } from "@/shared/api/ai";
import { useListQuery } from "@/shared/hooks/useListQuery";
import type { AiDatasetDto } from "@/shared/types";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

export function AiLabeling() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [markCorrect, setMarkCorrect] = useState(false);
  const [questionKw, setQuestionKw] = useState("");

  const { data: datasets, loading } = useListQuery<AiDatasetDto>(
    ["api", "ai", "datasets", "list"],
    () => datasetApi.list(),
    { staleTime: 5 * 60 * 1000 },
  );

  const filtered = datasets.filter((d) => {
    const kw = questionKw.trim().toLowerCase();
    if (kw && !d.name.toLowerCase().includes(kw)) return false;
    return true;
  });

  const selected = datasets.find((d) => d.id === selectedId) ?? null;

  const cols = [
    {
      key: "name",
      title: "所属知识库",
      render: (d: AiDatasetDto) => (
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {d.name}
        </Typography>
      ),
    },
    {
      key: "source",
      title: "用户标识",
      render: (d: AiDatasetDto) => (
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          {d.source}
        </Typography>
      ),
    },
    {
      key: "description",
      title: "问题",
      render: (d: AiDatasetDto) => (
        <Typography variant="caption" sx={{ color: "text.secondary" }} noWrap>
          {d.description || d.name}
        </Typography>
      ),
    },
    {
      key: "enabled",
      title: "已标注",
      render: (d: AiDatasetDto) => <StatusChip enabled={d.enabled} />,
    },
    {
      key: "records",
      title: "调用结果",
      render: (d: AiDatasetDto) => (
        <Chip label={`${d.records} 条`} size="small" variant="outlined" />
      ),
    },
  ];

  return (
    <PageStack>
      <AiPageHeader title="数据标注" subtitle="左侧选择问题，右侧查看并标注 AI 智能回答。" />

      <ContentCard sx={{ p: 2 }}>
        <Box
          sx={{
            display: "flex",
            gap: 1.5,
            flexWrap: "wrap",
            alignItems: "center",
            "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
          }}
        >
          <TextField
            placeholder="请输入问题"
            value={questionKw}
            onChange={(e) => setQuestionKw(e.target.value)}
            fullWidth={false}
            sx={{ width: 220 }}
          />
          <Select
            label="是否标注"
            value=""
            onChange={() => undefined}
            options={[{ value: "", label: "全部" }]}
            fullWidth={false}
            sx={{ width: 160 }}
          />
          <Select
            label="调用结果"
            value=""
            onChange={() => undefined}
            options={[{ value: "", label: "全部" }]}
            fullWidth={false}
            sx={{ width: 160 }}
          />
          <Button variant="contained">查询</Button>
          <Button variant="outlined" color="default" onClick={() => setQuestionKw("")}>
            重置
          </Button>
        </Box>
      </ContentCard>

      <Box
        sx={{
          display: "flex",
          gap: 2.5,
          alignItems: "stretch",
          flexWrap: { xs: "wrap", lg: "nowrap" },
        }}
      >
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <DataTable
            columns={cols}
            data={filtered}
            loading={loading}
            rowKey={(d) => d.id}
            onRowClick={(d) => setSelectedId(d.id)}
          />
        </Box>

        {/* ── Right: 智能标注 panel ── */}
        <Box sx={{ width: { xs: "100%", lg: 380 }, flexShrink: 0 }}>
          <ContentCard sx={{ height: "100%" }}>
            <Stack
              direction="row"
              sx={{ alignItems: "center", justifyContent: "space-between", mb: 2 }}
            >
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "primary.main" }} />
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  智能标注
                </Typography>
              </Stack>
              <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  标注为正确答案
                </Typography>
                <Switch checked={markCorrect} onChange={setMarkCorrect} />
              </Stack>
            </Stack>

            <Box
              sx={{
                bgcolor: "action.hover",
                borderRadius: 1.5,
                p: 1.5,
                mb: 2,
              }}
            >
              <Typography variant="body2">
                {selected ? selected.name : "请在左侧选择一条问题"}
              </Typography>
            </Box>

            <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
              <LightbulbIcon sx={{ color: "warning.main" }} />
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                AI 智能回答
              </Typography>
              <Chip label="支持 Markdown" size="small" variant="outlined" />
            </Stack>

            <Typography
              variant="body2"
              sx={{ color: "text.secondary", whiteSpace: "pre-wrap", lineHeight: 1.7 }}
            >
              {selected?.description ||
                "选中左侧问题后，这里将展示 AI 生成的参考答案，可在此核对并标注为正确答案。"}
            </Typography>
          </ContentCard>
        </Box>
      </Box>
    </PageStack>
  );
}

export default AiLabeling;
