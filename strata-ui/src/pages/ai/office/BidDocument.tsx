// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Card, CardContent, Stack, Typography, IconButton } from "@mui/material";
import { Button, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { Visibility, Edit, Delete, Description } from "@mui/icons-material";

interface TemplateItem {
  id: number;
  name: string;
  description: string;
  date: string;
}

const DEFAULT_ITEMS: TemplateItem[] = [
  {
    id: 1,
    name: "标书生成助手",
    description: "根据招标要求自动生成投标书，包含公司简介、技术方案、商务报价和资质文件。",
    date: "2025-10-14",
  },
];

export function BidDocument() {
  const [items] = useState(DEFAULT_ITEMS);
  const [filter, setFilter] = useState("");
  const filtered = items.filter((i) => i.name.includes(filter));

  return (
    <PageStack>
      <Box
        sx={{
          display: "flex",
          gap: 1.5,
          mb: 2,
          alignItems: "center",
          flexWrap: "wrap",
          "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
        }}
      >
        <Typography variant="h6">AI 标书</Typography>
        <TextField
          placeholder="请输入标题"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <Button variant="contained">新增</Button>
      </Box>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))",
          gap: 2,
        }}
      >
        {filtered.map((item) => (
          <Card key={item.id} variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                <Box
                  sx={{
                    width: 44,
                    height: 44,
                    borderRadius: 1.5,
                    bgcolor: "success.main",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "success.contrastText",
                  }}
                >
                  <Description />
                </Box>
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {item.name}
                </Typography>
              </Box>
              <Typography
                variant="body2"
                sx={{
                  color: "text.secondary",
                  mt: 1.5,
                  display: "-webkit-box",
                  WebkitLineClamp: 3,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                }}
              >
                {item.description}
              </Typography>
              <Stack
                direction="row"
                sx={{
                  mt: 1.5,
                  pt: 1.5,
                  borderTop: 1,
                  borderColor: "divider",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <Stack direction="row" spacing={0.5}>
                  <IconButton size="small">
                    <Visibility fontSize="small" />
                  </IconButton>
                  <IconButton size="small">
                    <Edit fontSize="small" />
                  </IconButton>
                  <IconButton size="small" color="error">
                    <Delete fontSize="small" />
                  </IconButton>
                </Stack>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  {item.date}
                </Typography>
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Box>
    </PageStack>
  );
}

export default BidDocument;
