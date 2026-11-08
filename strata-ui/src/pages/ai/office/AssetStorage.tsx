// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import {
  CloudUpload as UploadIcon,
  Folder as FolderIcon,
  Schedule as ClockIcon,
  Add as AddIcon,
  Search as SearchIcon,
  ImageSearch as ImageSearchIcon,
  Image as ImagePlaceholderIcon,
  Visibility as ViewIcon,
  Download as DownloadIcon,
  FolderOpen as MoveIcon,
  DeleteOutlined as DeleteIcon,
} from "@mui/icons-material";
import { Box, Checkbox, Chip, IconButton, Paper, Stack, Typography } from "@mui/material";
import { Button, Dialog, Select, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { toast } from "@/shared/pageKit/Toast";

interface AssetImage {
  id: string;
  name: string;
  size: string;
  time: string;
  group: string;
}

const SYSTEM_GROUPS = [
  { id: "all", label: "所有文件", icon: FolderIcon, count: 0 },
  { id: "recent", label: "最近上传", icon: ClockIcon, count: 0 },
];

const MY_GROUPS = [
  { id: "scene", label: "风景摄影" },
  { id: "people", label: "人物肖像" },
  { id: "animal", label: "动物摄影" },
];

export function AssetStorage() {
  const [images, setImages] = useState<AssetImage[]>([]);
  const [activeGroup, setActiveGroup] = useState("all");
  const [keyword, setKeyword] = useState("");
  const [uploadOpen, setUploadOpen] = useState(false);

  // multi-select
  const [selectMode, setSelectMode] = useState(false);
  const [checked, setChecked] = useState<Set<string>>(new Set());

  const filtered = images.filter(
    (img) => !keyword || img.name.toLowerCase().includes(keyword.toLowerCase()),
  );

  const toggleCheck = (id: string) =>
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  function confirmUpload() {
    const now = new Date().toISOString().replace("T", " ").slice(0, 19);
    setImages((prev) => [
      {
        id: `img-${Date.now()}`,
        name: `upload-${prev.length + 1}.jpg`,
        size: "2.00 MB",
        time: now,
        group: activeGroup,
      },
      ...prev,
    ]);
    setUploadOpen(false);
    toast.success("上传成功");
  }

  return (
    <PageStack>
      <Box sx={{ display: "flex", gap: 2, alignItems: "flex-start" }}>
        {/* Left group tree */}
        <Paper variant="outlined" sx={{ width: 260, flexShrink: 0, p: 2 }}>
          <Button
            variant="contained"
            fullWidth
            startIcon={<UploadIcon />}
            onClick={() => setUploadOpen(true)}
          >
            上传图片
          </Button>
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            系统分组
          </Typography>
          <Stack spacing={0.5}>
            {SYSTEM_GROUPS.map((g) => (
              <Box
                key={g.id}
                onClick={() => setActiveGroup(g.id)}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  px: 1.5,
                  py: 1,
                  borderRadius: 1,
                  cursor: "pointer",
                  bgcolor: activeGroup === g.id ? "primary.light" : "transparent",
                  color: activeGroup === g.id ? "primary.contrastText" : "text.primary",
                  "&:hover": { bgcolor: activeGroup === g.id ? "primary.light" : "action.hover" },
                }}
              >
                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <g.icon fontSize="small" />
                  <Typography variant="body2">{g.label}</Typography>
                </Stack>
                <Chip label={images.length} size="small" />
              </Box>
            ))}
          </Stack>
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mt: 2, mb: 1 }}
          >
            <Typography variant="subtitle2">我的分组</Typography>
            <IconButton size="small">
              <AddIcon fontSize="small" />
            </IconButton>
          </Stack>
          <Stack spacing={0.5}>
            {MY_GROUPS.map((g) => (
              <Box
                key={g.id}
                onClick={() => setActiveGroup(g.id)}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  px: 1.5,
                  py: 1,
                  borderRadius: 1,
                  cursor: "pointer",
                  bgcolor: activeGroup === g.id ? "primary.light" : "transparent",
                  "&:hover": { bgcolor: "action.hover" },
                }}
              >
                <FolderIcon fontSize="small" sx={{ color: "text.secondary" }} />
                <Typography variant="body2">{g.label}</Typography>
              </Box>
            ))}
          </Stack>
        </Paper>

        {/* Right content */}
        <Box sx={{ flex: 1, minWidth: 0 }}>
          {/* search bar */}
          <Box
            sx={{
              display: "flex",
              gap: 1.5,
              alignItems: "center",
              mb: 2,
              flexWrap: "wrap",
              "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 140 },
            }}
          >
            <TextField
              placeholder="按关键字、标签搜索"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              fullWidth={false}
              sx={{ width: 280 }}
              slotProps={{
                input: {
                  startAdornment: (
                    <SearchIcon fontSize="small" sx={{ mr: 1, color: "text.secondary" }} />
                  ),
                },
              }}
            />
            <Button variant="outlined" color="default" startIcon={<ImageSearchIcon />}>
              以图搜图
            </Button>
            <Select
              label="类型"
              value="all"
              onChange={() => undefined}
              fullWidth={false}
              sx={{ width: 140 }}
              options={[
                { value: "all", label: "全部类型" },
                { value: "jpg", label: "JPG" },
                { value: "png", label: "PNG" },
              ]}
            />
            <Select
              label="排序"
              value="latest"
              onChange={() => undefined}
              fullWidth={false}
              sx={{ width: 140 }}
              options={[
                { value: "latest", label: "最新优先" },
                { value: "oldest", label: "最早优先" },
              ]}
            />
          </Box>

          {/* multi-select bar */}
          {(selectMode || checked.size > 0) && (
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                mb: 2,
                px: 2,
                py: 1,
                bgcolor: "action.hover",
                borderRadius: 1,
              }}
            >
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                已选 {checked.size} 个文件
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button
                  size="small"
                  variant="outlined"
                  color="default"
                  onClick={() => {
                    setChecked(new Set());
                    setSelectMode(false);
                  }}
                >
                  取消选择
                </Button>
                <Button size="small" variant="outlined" color="default" startIcon={<MoveIcon />}>
                  移动({checked.size})
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  color="danger"
                  startIcon={<DeleteIcon />}
                  onClick={() => {
                    setImages((prev) => prev.filter((i) => !checked.has(i.id)));
                    setChecked(new Set());
                    toast.success("已删除");
                  }}
                >
                  删除({checked.size})
                </Button>
              </Stack>
            </Box>
          )}

          {/* image grid */}
          {filtered.length === 0 ? (
            <Paper variant="outlined" sx={{ p: 8, textAlign: "center", color: "text.secondary" }}>
              <UploadIcon sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
              <Typography variant="body2">暂无图片，点击左上角「上传图片」开始</Typography>
            </Paper>
          ) : (
            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: {
                  xs: "1fr",
                  sm: "repeat(2, 1fr)",
                  md: "repeat(3, 1fr)",
                  lg: "repeat(4, 1fr)",
                },
                gap: 2,
              }}
            >
              {filtered.map((img) => (
                <Paper
                  key={img.id}
                  variant="outlined"
                  sx={{
                    borderRadius: 2,
                    overflow: "hidden",
                    borderColor: checked.has(img.id) ? "primary.main" : "divider",
                    borderWidth: checked.has(img.id) ? 2 : 1,
                  }}
                >
                  <Box
                    sx={{
                      position: "relative",
                      aspectRatio: "4 / 3",
                      bgcolor: "grey.200",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                    }}
                  >
                    <ImagePlaceholderIcon sx={{ fontSize: 40, color: "text.disabled" }} />
                    <Checkbox
                      size="small"
                      checked={checked.has(img.id)}
                      onClick={() => toggleCheck(img.id)}
                      sx={{
                        position: "absolute",
                        top: 4,
                        left: 4,
                        bgcolor: "background.paper",
                        borderRadius: 0.5,
                      }}
                    />
                    <Stack
                      direction="row"
                      spacing={0.5}
                      sx={{
                        position: "absolute",
                        bottom: 8,
                        right: 8,
                        opacity: 0,
                        transition: "opacity 0.2s",
                        bgcolor: "background.paper",
                        borderRadius: 1,
                        p: 0.5,
                        ".MuiPaper-root:hover &": { opacity: 1 },
                      }}
                    >
                      {[ViewIcon, DownloadIcon, MoveIcon].map((Icon, i) => (
                        <IconButton key={i} size="small">
                          <Icon fontSize="small" />
                        </IconButton>
                      ))}
                      <IconButton size="small">
                        <DeleteIcon fontSize="small" sx={{ color: "error.main" }} />
                      </IconButton>
                    </Stack>
                  </Box>
                  <Box sx={{ p: 1.5 }}>
                    <Typography variant="body2" noWrap>
                      {img.name}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "text.secondary" }}>
                      {img.size} · {img.time}
                    </Typography>
                  </Box>
                </Paper>
              ))}
            </Box>
          )}
        </Box>
      </Box>

      {/* Upload dialog */}
      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)} title="上传图片" maxWidth="sm">
        <Box
          sx={{
            border: 2,
            borderStyle: "dashed",
            borderColor: "divider",
            borderRadius: 2,
            py: 6,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 1,
            cursor: "pointer",
          }}
          onClick={() => toast.info("请选择图片文件")}
        >
          <UploadIcon sx={{ fontSize: 40, color: "primary.main" }} />
          <Typography variant="body2">拖拽图片到此处，或点击上传</Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            大小 ≤ 2MB，支持 png / jpg / jpeg
          </Typography>
        </Box>
        <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setUploadOpen(false)}>
            关闭
          </Button>
          <Button variant="contained" onClick={confirmUpload}>
            上传
          </Button>
        </Box>
      </Dialog>
    </PageStack>
  );
}

export default AssetStorage;
