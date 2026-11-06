// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import {
  Box,
  Checkbox,
  CircularProgress,
  IconButton,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import AssignmentOutlinedIcon from "@mui/icons-material/AssignmentOutlined";
import ContentCopyOutlinedIcon from "@mui/icons-material/ContentCopyOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import PlaylistAddCheckOutlinedIcon from "@mui/icons-material/PlaylistAddCheckOutlined";
import UploadFileOutlinedIcon from "@mui/icons-material/UploadFileOutlined";
import { Button, Pagination, Select, Switch, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { aiToolApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";
import { failureMessage } from "@/shared/hooks/useCrud";

interface OcrTemplate {
  id: string;
  name: string;
  desc: string;
  fields: string[];
  date: string;
}

const TEMPLATES: OcrTemplate[] = [
  {
    id: "license",
    name: "营业执照识别",
    desc: "从给定的图片中获取企业名称、城市时间信息",
    fields: ["company_name", "create_time"],
    date: "2024-12-16",
  },
  {
    id: "bankcard",
    name: "银行卡信息读取",
    desc: "从用户提供的图片中获取银行卡的所属银行、卡号信息",
    fields: ["bank_name", "card_no"],
    date: "2024-12-29",
  },
  {
    id: "idcard",
    name: "身份证信息解析",
    desc: "读取身份证图片中的姓名、身份证号、住址信息",
    fields: ["name", "id_no", "address"],
    date: "2024-12-29",
  },
  {
    id: "plate",
    name: "车牌识别",
    desc: "识别图片中的车牌号，如果有多张车牌，取最前景最清晰的车牌",
    fields: ["plate_no"],
    date: "2024-12-29",
  },
  {
    id: "invoice",
    name: "发票信息解析",
    desc: "提取发票图片中的金额、发票号码、地区关键信息",
    fields: ["amount", "invoice_no", "region"],
    date: "2024-12-29",
  },
];

export function OcrTemplates() {
  const [active, setActive] = useState<OcrTemplate | null>(null);
  const [filterTitle, setFilterTitle] = useState("");
  const [filterPrompt, setFilterPrompt] = useState("");
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [advanced, setAdvanced] = useState(false);
  const [page, setPage] = useState(1);
  const pageSize = 8;

  const filtered = TEMPLATES.filter(
    (t) =>
      (!filterTitle || t.name.includes(filterTitle)) &&
      (!filterPrompt || t.desc.includes(filterPrompt)),
  );

  async function run() {
    const trimmed = url.trim();
    if (!trimmed || busy) return;
    setBusy(true);
    setResult(null);
    try {
      const res = await aiToolApi.run("ocr", trimmed);
      setResult(res.result);
    } catch (err) {
      toast.error(failureMessage(err));
    } finally {
      setBusy(false);
    }
  }

  // 尝试把结果解析为结构化字段，否则退化为纯文本
  const parsedFields: { key: string; value: string }[] = (() => {
    if (!result) return [];
    try {
      const obj = JSON.parse(result);
      if (obj && typeof obj === "object") {
        return Object.entries(obj).map(([k, v]) => ({ key: k, value: String(v) }));
      }
    } catch {}
    return [];
  })();

  if (active) {
    return (
      <PageStack>
        <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1 }}>
          <IconButton
            size="small"
            onClick={() => {
              setActive(null);
              setResult(null);
              setUrl("");
            }}
          >
            <ArrowBackOutlinedIcon />
          </IconButton>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            {active.name}
          </Typography>
        </Stack>
        <Box
          sx={{
            display: "flex",
            gap: 2,
            alignItems: "flex-start",
            minHeight: "calc(100vh - 220px)",
          }}
        >
          {/* Left: upload */}
          <Paper
            variant="outlined"
            sx={{
              flex: 1,
              p: 3,
              borderRadius: 3,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
            }}
          >
            <Box
              sx={{
                width: 320,
                height: 360,
                border: 1,
                borderStyle: "dashed",
                borderColor: "divider",
                borderRadius: 3,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                gap: 1,
                mt: 3,
              }}
            >
              {busy ? (
                <CircularProgress />
              ) : (
                <>
                  <UploadFileOutlinedIcon sx={{ fontSize: 40, color: "text.disabled" }} />
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    拖拽图片到此处上传
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.disabled" }}>
                    或点击选择文件
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.disabled" }}>
                    支持 PNG 格式
                  </Typography>
                </>
              )}
            </Box>
            <Typography variant="caption" sx={{ color: "primary.main", cursor: "pointer", mt: 2 }}>
              下载示例图片
            </Typography>
            <TextField
              size="small"
              fullWidth
              sx={{ mt: 2, maxWidth: 420 }}
              placeholder="或粘贴图片 URL 后点击解析数据"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              disabled={busy}
            />
          </Paper>

          {/* Right: result */}
          <Box sx={{ width: 380, display: "flex", flexDirection: "column", gap: 2 }}>
            <Stack direction="row" spacing={1}>
              <Button
                variant="contained"
                sx={{ flex: 1 }}
                disabled={!url.trim() || busy}
                startIcon={<PlaylistAddCheckOutlinedIcon sx={{ fontSize: 18 }} />}
                onClick={() => void run()}
              >
                {busy ? "解析中..." : "解析数据"}
              </Button>
              <Button
                variant="outlined"
                color="default"
                startIcon={<DescriptionOutlinedIcon sx={{ fontSize: 18 }} />}
                onClick={() => {
                  setResult(null);
                  setUrl("");
                }}
              >
                清空图片
              </Button>
            </Stack>

            <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 3 }}>
              <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
                <Box sx={{ width: 200 }}>
                  <Select
                    value="qwen3.7-flash-c"
                    onChange={() => {}}
                    options={[
                      { value: "qwen3.7-flash-c", label: "qwen3.7-flash-c" },
                      { value: "qwen3.7-plus", label: "Aliyun:qwen3.7-plus" },
                    ]}
                  />
                </Box>
                <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    {advanced ? "高级" : "快速"}
                  </Typography>
                  <Switch checked={advanced} onChange={setAdvanced} />
                </Stack>
              </Stack>
            </Paper>

            {/* Result field cards */}
            {(parsedFields.length > 0
              ? parsedFields
              : active.fields.map((f) => ({ key: f, value: "" }))
            ).map((f, i) => (
              <Paper key={f.key} variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    {i + 1}
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600, flex: 1 }}>
                    {f.key}
                  </Typography>
                  <IconButton size="small">
                    <ContentCopyOutlinedIcon sx={{ fontSize: 15 }} />
                  </IconButton>
                </Stack>
                <Typography
                  variant="body2"
                  sx={{ color: f.value ? "text.primary" : "text.disabled", mt: 0.5 }}
                >
                  {f.value || "—"}
                </Typography>
              </Paper>
            ))}

            {result && parsedFields.length === 0 && (
              <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                <Typography
                  variant="body2"
                  sx={{ whiteSpace: "pre-wrap", fontFamily: "monospace", fontSize: 12 }}
                >
                  {result}
                </Typography>
              </Paper>
            )}
          </Box>
        </Box>
      </PageStack>
    );
  }

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
        <Typography variant="body2">标题</Typography>
        <TextField
          size="small"
          placeholder="请输入标题"
          value={filterTitle}
          onChange={(e) => setFilterTitle(e.target.value)}
        />
        <Typography variant="body2" sx={{ ml: 1 }}>
          提示词
        </Typography>
        <TextField
          size="small"
          placeholder="请输入提示词"
          value={filterPrompt}
          onChange={(e) => setFilterPrompt(e.target.value)}
        />
        <Button size="small" variant="contained">
          查询
        </Button>
        <Button
          size="small"
          variant="outlined"
          color="default"
          onClick={() => {
            setFilterTitle("");
            setFilterPrompt("");
          }}
        >
          重置
        </Button>
      </Box>

      {/* Action bar */}
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Stack direction="row" spacing={1}>
          <Button
            size="small"
            variant="contained"
            startIcon={<DescriptionOutlinedIcon sx={{ fontSize: 16 }} />}
          >
            新增
          </Button>
          <Button size="small" variant="outlined" color="default">
            删除
          </Button>
        </Stack>
        <Stack direction="row" spacing={0.5}>
          <IconButton size="small">
            <DescriptionOutlinedIcon />
          </IconButton>
          <IconButton size="small">
            <PlaylistAddCheckOutlinedIcon />
          </IconButton>
        </Stack>
      </Box>

      {/* Card grid */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
          gap: 2,
        }}
      >
        {filtered.slice((page - 1) * pageSize, page * pageSize).map((t) => (
          <Paper
            key={t.id}
            variant="outlined"
            sx={{ borderRadius: 3, p: 2, cursor: "pointer" }}
            onClick={() => setActive(t)}
          >
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "center", mb: 1.5 }}>
              <Box
                sx={{
                  width: 44,
                  height: 44,
                  borderRadius: 2,
                  bgcolor: "secondary.main",
                  color: "#fff",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontWeight: 700,
                }}
              >
                {t.name[0]}
              </Box>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                {t.name}
              </Typography>
            </Stack>
            <Typography variant="body2" sx={{ color: "text.secondary", minHeight: 48 }}>
              {t.desc}
            </Typography>
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                mt: 2,
                pt: 1.5,
                borderTop: 1,
                borderColor: "divider",
              }}
            >
              <Stack direction="row" spacing={0.5}>
                <IconButton size="small">
                  <DescriptionOutlinedIcon sx={{ fontSize: 17, color: "primary.main" }} />
                </IconButton>
                <IconButton size="small">
                  <EditOutlinedIcon sx={{ fontSize: 17, color: "primary.main" }} />
                </IconButton>
                <IconButton size="small">
                  <ContentCopyOutlinedIcon sx={{ fontSize: 17, color: "primary.main" }} />
                </IconButton>
                <IconButton size="small">
                  <AssignmentOutlinedIcon sx={{ fontSize: 17, color: "error.main" }} />
                </IconButton>
              </Stack>
              <Checkbox size="small" />
            </Box>
            <Typography variant="caption" sx={{ color: "text.disabled" }}>
              {t.date}
            </Typography>
          </Paper>
        ))}
      </Box>

      <Pagination
        total={filtered.length}
        page={page}
        pageSize={pageSize}
        onPage={setPage}
        onPageSize={() => {}}
      />

      <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          共 {filtered.length} 条
        </Typography>
      </Box>
    </PageStack>
  );
}

export default OcrTemplates;
