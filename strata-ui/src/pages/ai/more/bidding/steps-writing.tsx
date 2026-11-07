// SPDX-License-Identifier: Apache-2.0
// Steps 4-5: write the response sections, then run the disqualification check.

import { Box, CircularProgress, LinearProgress, Paper, Typography } from "@mui/material";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import DescriptionIcon from "@mui/icons-material/Description";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { Button, Chip } from "@/components/ui/index";
import { OutlineRow, RISKS } from "./content";
import { Caption, StatChip, Dot } from "./shared";
import { Bidding } from "./main";

export function WriteStep({
  state,
  chapters,
  selected,
  onSelect,
  onRun,
}: {
  state: "idle" | "running" | "done";
  chapters: OutlineRow[];
  selected: string;
  onSelect: (id: string) => void;
  onRun: () => void;
}) {
  const sel = chapters.find((c) => c.num === selected) ?? chapters[0];
  return (
    <Box>
      <Box sx={{ display: "flex", alignItems: "center", gap: 2, mb: 2, flexWrap: "wrap" }}>
        <Dot color="success.main" />
        <Caption>
          已完成 {state === "done" ? 1 : 0} / {chapters.length}
        </Caption>
        <Dot color="primary.main" />
        <Caption>进行中 {state === "running" ? 1 : 0}</Caption>
        <Dot color="text.disabled" />
        <Caption>待生成 {state === "done" ? chapters.length - 1 : chapters.length}</Caption>
        <Dot color="error.main" />
        <Caption>失败 0 · 0%</Caption>
        <Box sx={{ flex: 1 }} />
        <Button
          variant="contained"
          size="small"
          startIcon={<AutoAwesomeIcon />}
          onClick={onRun}
          disabled={state === "running"}
        >
          {state === "running" ? "重试未完成章节" : "AI 生成未完成章节"}
        </Button>
      </Box>

      <Box sx={{ display: "flex", gap: 2, alignItems: "stretch" }}>
        <Paper variant="outlined" sx={{ width: 320, p: 2, borderRadius: 2.5 }}>
          <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
              章节目录
            </Typography>
            <Box sx={{ flex: 1 }} />
            <Caption>{chapters.length} 个可写章节</Caption>
          </Box>
          {chapters.map((c) => (
            <Box
              key={c.id}
              onClick={() => onSelect(c.num)}
              sx={{
                display: "flex",
                alignItems: "center",
                gap: 1,
                py: 1,
                pl: 1,
                cursor: "pointer",
                borderLeft: c.num === selected ? 3 : 0,
                borderColor: "primary.main",
                bgcolor: c.num === selected ? "action.hover" : "transparent",
                borderRadius: 1,
              }}
            >
              <Caption sx={{ width: 44 }}>{c.num}</Caption>
              <Typography variant="body2" sx={{ flex: 1 }}>
                {c.title}
              </Typography>
              <Chip
                label={
                  state === "done" && c.num === selected
                    ? "已完成"
                    : state === "running" && c.num === selected
                      ? "进行中"
                      : "待生成"
                }
                size="small"
                variant="outlined"
                sx={{ height: 18, fontSize: 11 }}
              />
            </Box>
          ))}
        </Paper>

        <Paper
          variant="outlined"
          sx={{ flex: 1, p: 2.5, borderRadius: 2.5, display: "flex", flexDirection: "column" }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
            <Chip label={sel?.num} sx={{ bgcolor: "primary.main", color: "#fff", height: 22 }} />
            <Typography variant="subtitle1" sx={{ fontWeight: 700, flex: 1 }}>
              {sel?.title}
            </Typography>
            <Button variant="outlined" size="small">
              编辑正文
            </Button>
            <Button
              variant="contained"
              size="small"
              startIcon={<AutoAwesomeIcon />}
              onClick={onRun}
              disabled={state === "running"}
            >
              {state === "done" ? "重新生成" : "生成本章"}
            </Button>
          </Box>

          <Box
            sx={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", py: 8 }}
          >
            {state === "running" ? (
              <Box sx={{ textAlign: "center" }}>
                <CircularProgress sx={{ mb: 1.5 }} />
                <Typography variant="body1" sx={{ fontWeight: 700 }}>
                  AI 正在撰写本章
                </Typography>
                <Caption>AI 正在生成 · 38% · 结果自动保存</Caption>
              </Box>
            ) : state === "done" ? (
              <Box sx={{ width: "100%" }}>
                <WriteSection title="响应结论">
                  经参数比对，本项目所需台式终端与笔记本的核心指标均满足或优于招标要求，国产
                  CPU、内存与配置全部响应。
                </WriteSection>
                <WriteSection title="实施说明">
                  供货后统一部署国产操作系统与办公软件，并完成业务系统和外设兼容验证，交付期内完成全部
                  250 台终端安装。
                </WriteSection>
                <WriteSection title="证明材料">
                  附 CPU 参数检测报告、兼容性认证证书与原厂质保函，证明页码以正式装订版本为准。
                </WriteSection>
              </Box>
            ) : (
              <Box sx={{ textAlign: "center" }}>
                <DescriptionIcon sx={{ fontSize: 44, color: "text.disabled", mb: 1 }} />
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  本章尚未生成正文
                </Typography>
                <Caption>可以单章生成，也可以批量生成全部未完成章节。</Caption>
              </Box>
            )}
          </Box>
          <Caption sx={{ borderTop: 1, borderColor: "divider", pt: 1, mt: 1 }}>
            {state === "done" ? "269 字符" : "0 字符"}
          </Caption>
        </Paper>
      </Box>
    </Box>
  );
}

export function WriteSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Box sx={{ mb: 2 }}>
      <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
        {title}
      </Typography>
      <Typography variant="body2" sx={{ lineHeight: 1.9 }}>
        {children}
      </Typography>
    </Box>
  );
}

export function CheckStep({
  state,
  onRun,
}: {
  state: "idle" | "running" | "done";
  onRun: () => void;
}) {
  const sevColor = (s: string) =>
    s === "严重风险" ? "error" : s === "高风险" ? "warning" : "info";
  return (
    <Box>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
        {state === "done" ? (
          <>
            <StatChip>发现 3 项线索</StatChip>
            <StatChip>高及严重风险 2 项</StatChip>
          </>
        ) : (
          <Chip label="废标检查执行中" color="primary" variant="outlined" sx={{ height: 24 }} />
        )}
        <Box sx={{ flex: 1 }} />
        <Button
          variant="contained"
          size="small"
          startIcon={<AutoAwesomeIcon />}
          onClick={onRun}
          disabled={state === "running"}
        >
          {state === "running" ? "正在检查…" : "重新检查"}
        </Button>
      </Box>

      {state === "running" ? (
        <Box>
          <Paper variant="outlined" sx={{ p: 2.5, mb: 2, bgcolor: "action.hover" }}>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
              <CircularProgress size={20} />
              <Box sx={{ flex: 1 }}>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                  正在逐项核验废标风险
                </Typography>
                <Caption>
                  系统正在对照招标文件中的废标条款与已完成标书正文，结果生成后会自动刷新。
                </Caption>
              </Box>
              <Typography sx={{ color: "primary.main", fontWeight: 700 }}>8%</Typography>
            </Box>
            <LinearProgress variant="determinate" value={8} sx={{ mt: 1.5 }} />
          </Paper>
          <Box sx={{ display: "flex", gap: 2 }}>
            {[
              ["废标条款", "核对无效投标与刚性条件", <DescriptionIcon key="i" />],
              ["已写正文", "检查承诺、报价、签章和参数响应", <DescriptionIcon key="i" />],
              ["证据定位", "回查章节与招标文件原文引用", <LocationOnIcon key="i" />],
              ["材料缺口", "识别待补证明、证书和附件", <WarningAmberIcon key="i" />],
            ].map(([t, d, icon]) => (
              <Paper key={t as string} variant="outlined" sx={{ flex: 1, p: 2 }}>
                <Box sx={{ color: "primary.main", mb: 0.5 }}>{icon}</Box>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                  {t as string}
                </Typography>
                <Caption>{d as string}</Caption>
              </Paper>
            ))}
          </Box>
        </Box>
      ) : (
        <Box>
          <Paper
            variant="outlined"
            sx={{
              p: 2,
              mb: 2,
              display: "flex",
              gap: 1.5,
              alignItems: "center",
              bgcolor: "warning.light",
            }}
          >
            <WarningAmberIcon sx={{ color: "warning.dark" }} />
            <Typography variant="body2">
              <Box component="span" sx={{ fontWeight: 700 }}>
                AI 结果须人工逐项复核：
              </Box>
              模型结论仅作为排查线索，不代表招标人或评标委员会的最终认定；提交前请回到原文并留存复核记录。
            </Typography>
          </Paper>

          {RISKS.map((r, idx) => (
            <Paper key={r.id} variant="outlined" sx={{ p: 2.5, mb: 2 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1.5 }}>
                <Caption sx={{ fontSize: 16 }}>{String(idx + 1).padStart(2, "0")}</Caption>
                <Typography variant="subtitle1" sx={{ fontWeight: 700, flex: 1 }}>
                  {r.title}
                </Typography>
                <Chip
                  label={r.severity}
                  color={sevColor(r.severity)}
                  variant="outlined"
                  sx={{ height: 22 }}
                />
              </Box>
              <Typography variant="body2" sx={{ color: "text.secondary", mb: 1.5 }}>
                {r.reason}
              </Typography>
              <Box sx={{ display: "flex", gap: 2 }}>
                <Box sx={{ flex: 1 }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    证据与定位
                  </Typography>
                  <Typography
                    variant="body2"
                    sx={{ color: "primary.main", fontWeight: 600, mt: 0.5 }}
                  >
                    {r.evidence}
                  </Typography>
                </Box>
                <Box sx={{ flex: 1 }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    风险原因
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    {r.reason}
                  </Typography>
                </Box>
                <Box sx={{ flex: 1 }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    处理建议
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    {r.suggestion}
                  </Typography>
                </Box>
              </Box>
            </Paper>
          ))}
        </Box>
      )}
    </Box>
  );
}

export default Bidding;
