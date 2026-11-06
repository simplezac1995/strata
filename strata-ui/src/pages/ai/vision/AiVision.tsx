// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Box, Card, CardContent, IconButton, Typography, CircularProgress } from "@mui/material";
import { Button, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Science as RunIcon,
} from "@mui/icons-material";
import { skillApi } from "@/shared/api/ai";
import type { AiSkillDto } from "@/shared/types";

export function AiVision() {
  const [skills, setSkills] = useState<AiSkillDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    skillApi
      .list()
      .then((s) => setSkills(s.filter((x) => x.type === "vision" || x.type === "ocr")))
      .catch(() => setSkills([]))
      .finally(() => setLoading(false));
  }, []);

  const fallback = [
    { id: "1", name: "营业执照识别", description: "从给定的图片中获取企业名称、城市时间信息" },
    {
      id: "2",
      name: "银行卡信息读取",
      description: "从用户提供的图片中获取银行卡的所属银行、卡号信息",
    },
    { id: "3", name: "身份证信息解析", description: "读取身份证图片中的姓名、身份证号、住址信息" },
    {
      id: "4",
      name: "车牌识别",
      description: "识别图片中的车牌号，如果有多张车牌号，取最清晰的车牌",
    },
    { id: "5", name: "发票信息解析", description: "提取发票图片中的金额、发票号码、地区关键信息" },
  ];

  const items = (
    skills.length > 0
      ? skills.map((s) => ({ id: s.id, name: s.name, description: s.description ?? "" }))
      : fallback
  ).filter((t) => !search || t.name.includes(search));

  return (
    <PageStack>
      <Box
        sx={{
          display: "flex",
          gap: 1.5,
          flexWrap: "wrap",
          alignItems: "center",
          mb: 2,
          "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
        }}
      >
        <TextField
          size="small"
          placeholder="请输入标题"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Button variant="contained" startIcon={<AddIcon />} sx={{ borderRadius: 2 }}>
          新增
        </Button>
      </Box>
      {loading ? (
        <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
            gap: 2,
          }}
        >
          {items.map((t) => (
            <Card key={t.id} variant="outlined" sx={{ borderRadius: 3 }}>
              <CardContent>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1.5 }}>
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      bgcolor: "primary.main",
                      color: "primary.contrastText",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: 20,
                      fontWeight: 700,
                    }}
                  >
                    {t.name[0]}
                  </Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                    {t.name}
                  </Typography>
                </Box>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{
                    minHeight: 44,
                    display: "-webkit-box",
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: "vertical",
                    overflow: "hidden",
                  }}
                >
                  {t.description}
                </Typography>
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 1,
                    mt: 2,
                    pt: 1.5,
                    borderTop: 1,
                    borderColor: "divider",
                  }}
                >
                  <IconButton size="small">
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small">
                    <RunIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" sx={{ ml: "auto", color: "error.main" }}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Box>
              </CardContent>
            </Card>
          ))}
        </Box>
      )}
    </PageStack>
  );
}
