// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import {
  Box,
  Divider,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import FormatBoldIcon from "@mui/icons-material/FormatBold";
import FormatItalicIcon from "@mui/icons-material/FormatItalic";
import FormatUnderlinedIcon from "@mui/icons-material/FormatUnderlined";
import FormatStrikethroughIcon from "@mui/icons-material/FormatStrikethrough";
import FormatListBulletedIcon from "@mui/icons-material/FormatListBulleted";
import FormatListNumberedIcon from "@mui/icons-material/FormatListNumbered";
import FormatQuoteIcon from "@mui/icons-material/FormatQuote";
import CodeIcon from "@mui/icons-material/Code";
import FormatAlignLeftIcon from "@mui/icons-material/FormatAlignLeft";
import FormatAlignCenterIcon from "@mui/icons-material/FormatAlignCenter";
import FormatAlignRightIcon from "@mui/icons-material/FormatAlignRight";
import FormatAlignJustifyIcon from "@mui/icons-material/FormatAlignJustify";
import FormatClearIcon from "@mui/icons-material/FormatClear";
import InsertLinkIcon from "@mui/icons-material/InsertLink";
import TableChartIcon from "@mui/icons-material/TableChart";
import AddPhotoAlternateIcon from "@mui/icons-material/AddPhotoAlternate";
import { EditorContent, useEditor, type Editor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { Table, TableRow, TableCell, TableHeader } from "@tiptap/extension-table";
import Placeholder from "@tiptap/extension-placeholder";
import TextAlign from "@tiptap/extension-text-align";
import { fileApi } from "@/shared/api/system";
import { toast } from "@/shared/pageKit/Toast";
import { useI18n } from "@/shared/i18n";
import { resolveUploadedObjectUrl } from "@/components/ui/upload/fileUrl";
import { Button } from "@/components/ui/Button";

export interface RichTextEditorProps {
  /** Controlled HTML string. */
  value: string;
  onChange: (html: string) => void;
  placeholder?: string;
  minHeight?: number;
  /** Override the default image upload (defaults to fileApi upload). */
  onUploadImage?: (file: File) => Promise<string>;
  /** Fired once the underlying Tiptap editor instance is ready. */
  onReady?: (editor: Editor) => void;
  disabled?: boolean;
}

async function defaultImageUpload(file: File): Promise<string> {
  const dto = await fileApi.upload(file, "uploads");
  return resolveUploadedObjectUrl(dto);
}

export function RichTextEditor({
  value,
  onChange,
  placeholder,
  minHeight = 240,
  onUploadImage,
  onReady,
  disabled,
}: RichTextEditorProps) {
  const { t } = useI18n();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [linkOpen, setLinkOpen] = useState(false);
  const [linkHref, setLinkHref] = useState("");

  // Keep callbacks in refs so useEditor doesn't rebuild on every render.
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const onUploadRef = useRef(onUploadImage ?? defaultImageUpload);
  const onReadyRef = useRef(onReady);
  onReadyRef.current = onReady;

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        codeBlock: {},
        link: { openOnClick: false, autolink: true, linkOnPaste: true },
      }),
      Image,
      Table.configure({ resizable: true }),
      TableRow,
      TableHeader,
      TableCell,
      TextAlign.configure({ types: ["heading", "paragraph"] }),
      Placeholder.configure({ placeholder: placeholder ?? t("editor.placeholder") }),
    ],
    content: value,
    editable: !disabled,
    onUpdate: ({ editor: e }) => onChangeRef.current(e.getHTML()),
    onCreate: ({ editor: e }) => onReadyRef.current?.(e),
  });

  // Sync external value changes back into the editor without looping.
  useEffect(() => {
    if (!editor) return;
    if (value !== editor.getHTML()) {
      editor.commands.setContent(value, { emitUpdate: false });
    }
  }, [value, editor]);

  useEffect(() => {
    editor?.setEditable(!disabled);
  }, [disabled, editor]);

  const run = (fn: (e: Editor) => void) => {
    if (!editor) return;
    fn(editor);
  };

  const handlePickImage = () => fileInputRef.current?.click();

  const handleImageFile = async (file: File | undefined) => {
    if (!file || !editor) return;
    try {
      const url = await onUploadRef.current(file);
      editor.chain().focus().setImage({ src: url }).run();
    } catch {
      toast.error("upload.error.failed");
    } finally {
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const openLinkDialog = () => {
    if (!editor) return;
    const prev = editor.getAttributes("link").href as string | undefined;
    setLinkHref(prev ?? "");
    setLinkOpen(true);
  };

  const applyLink = () => {
    if (!editor) return;
    const href = linkHref.trim();
    if (href) {
      editor.chain().focus().extendMarkRange("link").setLink({ href }).run();
    } else {
      editor.chain().focus().unsetLink().run();
    }
    setLinkOpen(false);
  };

  const active = (name: string, attrs?: Record<string, unknown>) =>
    editor?.isActive(name, attrs) ?? false;

  const ToolBtn = ({
    label,
    onClick,
    isActive,
    children,
  }: {
    label: string;
    onClick: () => void;
    isActive?: boolean;
    children: ReactNode;
  }) => (
    <Tooltip title={label} enterDelay={400}>
      <IconButton
        size="small"
        aria-label={label}
        onClick={onClick}
        sx={{
          borderRadius: 0.75,
          ...(isActive
            ? { bgcolor: "action.selected", color: "primary.main" }
            : { color: "text.secondary" }),
        }}
      >
        {children}
      </IconButton>
    </Tooltip>
  );

  const HeadingBtn = (level: 1 | 2 | 3) => (
    <IconButton
      size="small"
      aria-label={t(`editor.h${level}`)}
      onClick={() => run((e) => e.chain().focus().toggleHeading({ level }).run())}
      sx={{
        px: 0.75,
        borderRadius: 0.75,
        ...(active("heading", { level })
          ? { bgcolor: "action.selected", color: "primary.main" }
          : { color: "text.secondary" }),
      }}
    >
      <Typography variant="caption" sx={{ fontWeight: 700, lineHeight: 1 }}>
        H{level}
      </Typography>
    </IconButton>
  );

  return (
    <Box
      sx={{
        border: (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: 1,
        bgcolor: "background.paper",
        "&:focus-within": {
          borderColor: "primary.main",
          boxShadow: (theme) => `0 0 0 1px ${theme.palette.primary.main}`,
        },
      }}
    >
      {/* Toolbar */}
      <Stack
        direction="row"
        spacing={0.25}
        sx={{
          p: 0.5,
          flexWrap: "wrap",
          borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
        }}
      >
        <ToolBtn
          label={t("editor.bold")}
          isActive={active("bold")}
          onClick={() => run((e) => e.chain().focus().toggleBold().run())}
        >
          <FormatBoldIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.italic")}
          isActive={active("italic")}
          onClick={() => run((e) => e.chain().focus().toggleItalic().run())}
        >
          <FormatItalicIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.underline")}
          isActive={active("underline")}
          onClick={() => run((e) => e.chain().focus().toggleUnderline().run())}
        >
          <FormatUnderlinedIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.strike")}
          isActive={active("strike")}
          onClick={() => run((e) => e.chain().focus().toggleStrike().run())}
        >
          <FormatStrikethroughIcon fontSize="small" />
        </ToolBtn>

        <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />

        {HeadingBtn(1)}
        {HeadingBtn(2)}
        {HeadingBtn(3)}

        <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />

        <ToolBtn
          label={t("editor.bulletList")}
          isActive={active("bulletList")}
          onClick={() => run((e) => e.chain().focus().toggleBulletList().run())}
        >
          <FormatListBulletedIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.orderedList")}
          isActive={active("orderedList")}
          onClick={() => run((e) => e.chain().focus().toggleOrderedList().run())}
        >
          <FormatListNumberedIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.blockquote")}
          isActive={active("blockquote")}
          onClick={() => run((e) => e.chain().focus().toggleBlockquote().run())}
        >
          <FormatQuoteIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.codeBlock")}
          isActive={active("codeBlock")}
          onClick={() => run((e) => e.chain().focus().toggleCodeBlock().run())}
        >
          <CodeIcon fontSize="small" />
        </ToolBtn>

        <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />

        <ToolBtn
          label={t("editor.alignLeft")}
          isActive={active("paragraph", { textAlign: "left" })}
          onClick={() => run((e) => e.chain().focus().setTextAlign("left").run())}
        >
          <FormatAlignLeftIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.alignCenter")}
          isActive={active("paragraph", { textAlign: "center" })}
          onClick={() => run((e) => e.chain().focus().setTextAlign("center").run())}
        >
          <FormatAlignCenterIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.alignRight")}
          isActive={active("paragraph", { textAlign: "right" })}
          onClick={() => run((e) => e.chain().focus().setTextAlign("right").run())}
        >
          <FormatAlignRightIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.alignJustify")}
          isActive={active("paragraph", { textAlign: "justify" })}
          onClick={() => run((e) => e.chain().focus().setTextAlign("justify").run())}
        >
          <FormatAlignJustifyIcon fontSize="small" />
        </ToolBtn>

        <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />

        <ToolBtn label={t("editor.link")} isActive={active("link")} onClick={openLinkDialog}>
          <InsertLinkIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.table")}
          onClick={() =>
            run((e) =>
              e.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(),
            )
          }
        >
          <TableChartIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn label={t("editor.image")} onClick={handlePickImage}>
          <AddPhotoAlternateIcon fontSize="small" />
        </ToolBtn>
        <ToolBtn
          label={t("editor.clearFormat")}
          onClick={() => run((e) => e.chain().focus().unsetAllMarks().clearNodes().run())}
        >
          <FormatClearIcon fontSize="small" />
        </ToolBtn>
      </Stack>

      {/* Content */}
      <Box
        sx={{
          px: 2,
          py: 1.5,
          minHeight,
          maxHeight: 480,
          overflow: "auto",
          "& .ProseMirror": {
            outline: "none",
            minHeight,
            fontSize: 14,
            "& p": { margin: "0.25em 0" },
            "& h1": { fontSize: "1.6em", fontWeight: 700, margin: "0.4em 0 0.2em" },
            "& h2": { fontSize: "1.35em", fontWeight: 700, margin: "0.4em 0 0.2em" },
            "& h3": { fontSize: "1.15em", fontWeight: 700, margin: "0.4em 0 0.2em" },
            "& blockquote": {
              borderLeft: "3px solid",
              borderColor: "divider",
              margin: "0.5em 0",
              paddingLeft: 1,
              color: "text.secondary",
            },
            "& pre": {
              bgcolor: "action.hover",
              p: 1,
              borderRadius: 0.5,
              overflow: "auto",
              "& code": { bgcolor: "transparent", p: 0 },
            },
            "& code": { bgcolor: "action.hover", px: 0.5, borderRadius: 0.5 },
            "& img": { maxWidth: "100%", borderRadius: 0.5 },
            "& table": {
              borderCollapse: "collapse",
              minWidth: "50%",
              "& td, & th": { border: "1px solid", borderColor: "divider", p: 0.5 },
            },
            "& p.is-editor-empty:first-of-type::before": {
              content: "attr(data-placeholder)",
              float: "left",
              height: 0,
              pointerEvents: "none",
              color: "text.disabled",
            },
          },
        }}
      >
        <EditorContent editor={editor} />
      </Box>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        hidden
        data-testid="rich-text-image-input"
        onChange={(e) => void handleImageFile(e.target.files?.[0])}
      />

      {/* Link dialog */}
      {linkOpen ? (
        <Box
          sx={{
            position: "fixed",
            inset: 0,
            zIndex: (theme) => theme.zIndex.modal,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            bgcolor: "rgba(0,0,0,0.32)",
          }}
          onClick={() => setLinkOpen(false)}
        >
          <Box
            onClick={(e) => e.stopPropagation()}
            sx={{
              width: 420,
              maxWidth: "92vw",
              bgcolor: "background.paper",
              p: 2,
              borderRadius: 1,
              boxShadow: 6,
            }}
          >
            <TextField
              autoFocus
              fullWidth
              size="small"
              label={t("editor.linkUrl")}
              value={linkHref}
              onChange={(e) => setLinkHref(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") applyLink();
              }}
              slotProps={{
                input: {
                  startAdornment: <InputAdornment position="start">https://</InputAdornment>,
                },
              }}
            />
            <Stack direction="row" spacing={1} sx={{ mt: 1.5, justifyContent: "flex-end" }}>
              <Button
                variant="outlined"
                color="default"
                size="sm"
                onClick={() => setLinkOpen(false)}
              >
                {t("common.cancel")}
              </Button>
              <Button variant="contained" size="sm" onClick={applyLink}>
                {t("common.confirm")}
              </Button>
            </Stack>
          </Box>
        </Box>
      ) : null}
    </Box>
  );
}

export default RichTextEditor;
