// SPDX-License-Identifier: Apache-2.0
// The bidding page container: step state, project rail and step routing.

import { useMemo, useState } from "react";
import { Box, IconButton, Paper, Step, StepLabel, Stepper, Typography } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import DownloadIcon from "@mui/icons-material/Download";
import RefreshIcon from "@mui/icons-material/Refresh";
import { Button, Chip, Select, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { officeApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";
import { failureMessage } from "@/shared/hooks/useCrud";
import {
  StepId,
  STEP_LABELS,
  TENDER_FILE,
  TENDER_TEXT,
  ReqEntry,
  RESPONSE_ENTRIES,
  SCORE_ENTRIES,
  INVALID_ENTRIES,
  OutlineRow,
  OUTLINE_ROWS,
  RECENT_PROJECTS,
} from "./content";
import { Caption, RailIcon } from "./shared";
import { TenderFileStep, InterpretStep } from "./steps-intake";
import { RequirementsStep, OutlineStep } from "./steps-analysis";
import { WriteStep, CheckStep } from "./steps-writing";

export function Bidding() {
  const [view, setView] = useState<"landing" | "wizard">("landing");
  const [activeStep, setActiveStep] = useState<StepId>(0);
  const [projectName, setProjectName] = useState("");
  const [category, setCategory] = useState("goods");

  // AI 解读
  const [interpretState, setInterpretState] = useState<"idle" | "running" | "done">("done");

  // 需求与评分
  const [reqTab, setReqTab] = useState<"response" | "score" | "invalid">("response");
  const [extractState, setExtractState] = useState<"idle" | "running" | "done">("done");
  const [responseEntries, setResponseEntries] = useState<ReqEntry[]>(RESPONSE_ENTRIES);
  const [scoreEntries, setScoreEntries] = useState<ReqEntry[]>(SCORE_ENTRIES);
  const [invalidEntries, setInvalidEntries] = useState<ReqEntry[]>(INVALID_ENTRIES);

  // 标书大纲
  const [outlineState, setOutlineState] = useState<"idle" | "generating" | "done">("done");
  const [outlineRows, setOutlineRows] = useState<OutlineRow[]>(OUTLINE_ROWS);

  // 专业撰写
  const [writeState, setWriteState] = useState<"idle" | "running" | "done">("done");
  const [selectedChapter, setSelectedChapter] = useState("1.1.1");

  // 废标检查
  const [checkState, setCheckState] = useState<"idle" | "running" | "done">("done");

  const doneSteps = useMemo(() => {
    const d: Record<StepId, boolean> = {
      0: true,
      1: interpretState === "done",
      2: extractState === "done",
      3: outlineState === "done",
      4: writeState === "done",
      5: checkState === "done",
    };
    return d;
  }, [interpretState, extractState, outlineState, writeState, checkState]);

  const statusChip = useMemo(() => {
    if (activeStep === 0) return { label: "待解读", color: "primary" as const };
    if (activeStep === 1)
      return interpretState === "done"
        ? { label: "已解读", color: "success" as const }
        : { label: "待解读", color: "primary" as const };
    if (activeStep === 2)
      return extractState === "done"
        ? { label: "响应项待确认", color: "warning" as const }
        : { label: "正在提取", color: "primary" as const };
    if (activeStep === 3)
      return outlineState === "done"
        ? { label: "大纲待确认", color: "warning" as const }
        : { label: "正在生成大纲", color: "primary" as const };
    if (activeStep === 4)
      return writeState === "done"
        ? { label: "标书草稿", color: "info" as const }
        : { label: "正在撰写", color: "primary" as const };
    return checkState === "done"
      ? { label: "检查完成", color: "success" as const }
      : { label: "正在检查", color: "primary" as const };
  }, [activeStep, interpretState, extractState, outlineState, writeState, checkState]);

  async function runBiddingAction(action: string, text: string) {
    try {
      const res = await officeApi.run(action, text);
      return res.content;
    } catch (err) {
      toast.error(failureMessage(err, "该标书动作后端暂未开放，已保持当前空态"));
      return null;
    }
  }

  function enterWizard(step: StepId = 0) {
    setActiveStep(step);
    setView("wizard");
  }

  /* ---------- 步骤内动作 ---------- */
  async function handleInterpret() {
    setInterpretState("running");
    const content = await runBiddingAction("bidding-interpret", TENDER_TEXT);
    setInterpretState("done");
    if (content) toast.success("AI 解读完成");
  }

  async function handleExtract() {
    setExtractState("running");
    await runBiddingAction("bidding-extract", TENDER_TEXT);
    setExtractState("done");
  }

  async function handleOutline() {
    setOutlineState("generating");
    await runBiddingAction("bidding-outline", TENDER_TEXT);
    setOutlineState("done");
  }

  async function handleWrite() {
    setWriteState("running");
    await runBiddingAction("bidding-write", "生成章节正文");
    setWriteState("done");
  }

  async function handleCheck() {
    setCheckState("running");
    await runBiddingAction("bidding-check", TENDER_TEXT);
    setCheckState("done");
  }

  /* ============================== 落地页：新建投标项目 ============================== */
  if (view === "landing") {
    return (
      <PageStack>
        <Box sx={{ display: "flex", gap: 2, alignItems: "stretch" }}>
          <Paper variant="outlined" sx={{ flex: 1, p: 4, overflowY: "auto", borderRadius: 3 }}>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                新建投标项目
              </Typography>
              <Chip label="演示模式" sx={{ bgcolor: "warning.light", color: "warning.dark" }} />
              <Box sx={{ flex: 1 }} />
              <Caption sx={{ mr: 1 }}>生成模型</Caption>
              <Select
                value="deepseek"
                sx={{ width: 220 }}
                options={[{ value: "deepseek", label: "DeepSeek-V3（演示）" }]}
              />
            </Box>

            <NewProjectRow index={1} title="上传招标文件" required>
              <Paper
                variant="outlined"
                sx={{ p: 3, borderStyle: "dashed", textAlign: "center", bgcolor: "action.hover" }}
              >
                <CloudUploadIcon sx={{ mr: 1, verticalAlign: "middle", color: "primary.main" }} />
                <Typography component="span" variant="body2">
                  {TENDER_FILE}
                </Typography>
                <Caption sx={{ textAlign: "center" }}>4 KB · 待上传</Caption>
              </Paper>
            </NewProjectRow>

            <NewProjectRow index={2} title="录入招标信息" required>
              <Box sx={{ display: "flex", gap: 1.5, alignItems: "center" }}>
                <TextField
                  placeholder="项目名称"
                  value={projectName}
                  onChange={(e) => setProjectName(e.target.value)}
                  sx={{ flex: 1 }}
                />
                <Select
                  value={category}
                  sx={{ width: 140 }}
                  onChange={(e) => setCategory(e.target.value)}
                  options={[
                    { value: "goods", label: "货物类" },
                    { value: "service", label: "服务类" },
                    { value: "project", label: "工程类" },
                  ]}
                />
              </Box>
            </NewProjectRow>

            <NewProjectRow index={3} title="录入知识库文件" optional>
              <Box sx={{ display: "flex", gap: 2 }}>
                <Box sx={{ flex: 1 }}>
                  <Caption sx={{ mb: 0.5, display: "block" }}>从知识库选择</Caption>
                  <Select value="" options={[{ value: "", label: "选择知识库" }]} />
                </Box>
                <Box sx={{ flex: 1 }}>
                  <Caption sx={{ mb: 0.5, display: "block" }}>上传引用资料</Caption>
                  <Button variant="outlined" fullWidth startIcon={<CloudUploadIcon />}>
                    选择文件
                  </Button>
                </Box>
              </Box>
            </NewProjectRow>

            <Box sx={{ display: "flex", justifyContent: "flex-end", mt: 3 }}>
              <Button variant="contained" size="large" onClick={() => enterWizard(0)}>
                创建项目并开始编制
              </Button>
            </Box>
          </Paper>

          <Paper variant="outlined" sx={{ width: 380, p: 2.5, borderRadius: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
              最近项目
            </Typography>
            <TextField placeholder="搜索项目名称" sx={{ mb: 1.5 }} />
            <Select value="all" sx={{ mb: 1.5 }} options={[{ value: "all", label: "全部状态" }]} />
            {RECENT_PROJECTS.map((r) => (
              <Paper key={r.name} variant="outlined" sx={{ p: 2, mb: 1.5 }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.5 }}>
                  <Typography variant="body2" sx={{ fontWeight: 600, flex: 1 }}>
                    {r.name}
                  </Typography>
                  <Chip label={r.status} color="success" variant="outlined" sx={{ height: 20 }} />
                </Box>
                <Chip label={r.tag} variant="outlined" sx={{ height: 20, mb: 1 }} />
                <Caption sx={{ mb: 1 }}>更新于 {r.updated}</Caption>
                <Box sx={{ display: "flex", alignItems: "center" }}>
                  <Typography
                    variant="body2"
                    sx={{ color: "primary.main", cursor: "pointer", flex: 1 }}
                    onClick={() => enterWizard(0)}
                  >
                    继续编制 →
                  </Typography>
                  <IconButton size="small" sx={{ color: "error.main" }}>
                    <DeleteOutlineIcon fontSize="small" />
                  </IconButton>
                </Box>
              </Paper>
            ))}
          </Paper>
        </Box>
      </PageStack>
    );
  }

  /* ============================== 向导视图 ============================== */
  const subStatus = (i: StepId): string => {
    if (doneSteps[i]) return "已完成";
    if (i === activeStep) return i === 1 ? "可开始" : i === 5 ? "进行中" : "进行中";
    return i > activeStep ? "未开始" : "可开始";
  };

  return (
    <PageStack>
      {/* 顶部标题栏 */}
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
          <IconButton size="small" onClick={() => setView("landing")}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            {projectName || "政务服务中心国产化办公终端设备采购项目"}
          </Typography>
          <Chip
            label={statusChip.label}
            color={statusChip.color}
            variant="outlined"
            sx={{ height: 22 }}
          />
          <Box sx={{ flex: 1 }} />
          <Button
            variant="outlined"
            size="small"
            startIcon={<RefreshIcon />}
            onClick={() => toast.success("common.refreshed")}
          >
            刷新
          </Button>
          <Button variant="contained" size="small" startIcon={<DownloadIcon />}>
            导出 Word
          </Button>
        </Box>
        <Caption sx={{ mt: 0.5 }}>
          项目版本 V{activeStep + 1} · {TENDER_FILE}
        </Caption>
      </Paper>

      <Box sx={{ display: "flex", gap: 2, alignItems: "stretch", minHeight: 0 }}>
        {/* 左侧编制流程 */}
        <Paper variant="outlined" sx={{ width: 220, p: 2, borderRadius: 3 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
            编制流程
          </Typography>
          <Stepper orientation="vertical" activeStep={activeStep} sx={{ p: 0 }} connector={null}>
            {STEP_LABELS.map((label, i) => (
              <Step key={label} completed={doneSteps[i as StepId]}>
                <StepLabel
                  slots={{ stepIcon: RailIcon }}
                  onClick={() => setActiveStep(i as StepId)}
                  sx={{
                    cursor: "pointer",
                    p: 0.5,
                    borderRadius: 2,
                    pl: 1,
                    ...(i === activeStep ? { bgcolor: "action.selected" } : {}),
                    "& .MuiStepLabel-label": { fontSize: 14 },
                  }}
                >
                  {label}
                  <Caption sx={{ mt: 0.25 }}>{subStatus(i as StepId)}</Caption>
                </StepLabel>
              </Step>
            ))}
          </Stepper>
        </Paper>

        {/* 右侧内容区 */}
        <Paper
          variant="outlined"
          sx={{ flex: 1, p: 3, borderRadius: 3, overflowY: "auto", minWidth: 0 }}
        >
          {activeStep === 0 && <TenderFileStep onNext={() => enterWizard(1)} />}
          {activeStep === 1 && (
            <InterpretStep state={interpretState} onRun={() => void handleInterpret()} />
          )}
          {activeStep === 2 && (
            <RequirementsStep
              tab={reqTab}
              onTab={setReqTab}
              extractState={extractState}
              responseEntries={responseEntries}
              scoreEntries={scoreEntries}
              invalidEntries={invalidEntries}
              setResponseEntries={setResponseEntries}
              setScoreEntries={setScoreEntries}
              setInvalidEntries={setInvalidEntries}
              onExtract={() => void handleExtract()}
            />
          )}
          {activeStep === 3 && (
            <OutlineStep
              state={outlineState}
              rows={outlineRows}
              onRows={setOutlineRows}
              onRun={() => void handleOutline()}
            />
          )}
          {activeStep === 4 && (
            <WriteStep
              state={writeState}
              chapters={outlineRows.filter((r) => r.level === 3)}
              selected={selectedChapter}
              onSelect={setSelectedChapter}
              onRun={() => void handleWrite()}
            />
          )}
          {activeStep === 5 && <CheckStep state={checkState} onRun={() => void handleCheck()} />}
        </Paper>
      </Box>
    </PageStack>
  );
}

/* ============================== 落地页行 ============================== */
export function NewProjectRow({
  index,
  title,
  required,
  optional,
  children,
}: {
  index: number;
  title: string;
  required?: boolean;
  optional?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Box sx={{ display: "flex", gap: 2, mb: 3 }}>
      <Box
        sx={{
          width: 28,
          height: 28,
          borderRadius: "50%",
          bgcolor: "primary.main",
          color: "#fff",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontWeight: 700,
          fontSize: 14,
          flexShrink: 0,
        }}
      >
        {index}
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
          {title}{" "}
          {required && (
            <Box component="span" sx={{ color: "error.main" }}>
              *
            </Box>
          )}
          {optional && (
            <Typography component="span" variant="caption" sx={{ color: "text.secondary", ml: 1 }}>
              可选
            </Typography>
          )}
        </Typography>
        <Box sx={{ mt: 1 }}>{children}</Box>
      </Box>
    </Box>
  );
}

/* ============================== 步骤 0：招标文件 ============================== */
