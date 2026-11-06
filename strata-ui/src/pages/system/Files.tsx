// SPDX-License-Identifier: Apache-2.0
import { useEffect, useMemo, useRef, useState } from "react";
import { Box, Divider, Grid, IconButton, Link, Stack, Tab, Tabs, Typography } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";
import {
  Folder as FolderIcon,
  GridView as GridViewIcon,
  Image as ImageIcon,
  UploadFile as UploadFileIcon,
  VideoLibrary as VideoIcon,
  ViewList as ViewListIcon,
} from "@mui/icons-material";
import { Button, DataTable, ExportCsvButton, PageHeader, Pagination } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { fileCenterApi, type FileRef } from "@/shared/api/files";
import { resolveUploadedObjectUrl } from "@/components/ui/upload/fileUrl";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import { InfoBanner } from "@/shared/pageKit/InfoBanner";
import type { FileInfoDto } from "@/shared/types";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { BucketTree } from "./files/BucketTree";
import { ImageThumb } from "./files/ImageThumb";
import { ALL_BUCKET, formatSize, isImage, isVideo, triggerDownload } from "./files/fileHelpers";
import type { MimeTab, ViewMode } from "./files/fileHelpers";
import { FilesDialogs } from "./files/FilesDialogs";
export { formatSize } from "./files/fileHelpers";

/** 45.2 KB / 1.2 MB style formatting. */

export function Files() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:file:create"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:file:delete"));

  const {
    data: files,
    loading,
    reload,
  } = useListQuery<FileInfoDto>(["api", "system", "files"], fileCenterApi.list);

  const [tab, setTab] = useState<MimeTab>("all");
  const [bucket, setBucket] = useState<string>(ALL_BUCKET);
  const [view, setView] = useState<ViewMode>("grid");
  const [page, setPage] = useState(1);
  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());
  const [preview, setPreview] = useState<FileInfoDto | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [deleting, setDeleting] = useState<FileInfoDto | null>(null);
  const [batchConfirmOpen, setBatchConfirmOpen] = useState(false);

  const inputRef = useRef<HTMLInputElement>(null);

  /* ── Upload ── */
  const uploadMutation = useMutationWithToast(
    (file: File) => fileCenterApi.upload(file),
    { success: "toast.uploaded", failure: "toast.uploadFailed" },
    { onSuccess: () => reload() },
  );

  const deleteMutation = useMutationWithToast(
    (ref: FileRef) => fileCenterApi.remove(ref.bucket, ref.key),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    { onSuccess: () => reload() },
  );

  const batchDeleteMutation = useMutationWithToast(
    (items: FileRef[]) => fileCenterApi.batchRemove(items),
    { success: "toast.deleted", failure: "toast.deleteFailed" },
    {
      onSuccess: () => {
        setSelectedKeys(new Set());
      },
    },
  );

  const rowKeyOf = (f: FileInfoDto) => `${f.bucket}/${f.key}`;

  const buckets = useMemo(() => {
    const set = new Set<string>();
    for (const f of files) set.add(f.bucket);
    return [...set].sort();
  }, [files]);

  const filtered = useMemo(() => {
    return files.filter((f) => {
      if (bucket !== ALL_BUCKET && f.bucket !== bucket) return false;
      if (tab === "image" && !isImage(f)) return false;
      if (tab === "video" && !isVideo(f)) return false;
      return true;
    });
  }, [files, bucket, tab]);

  const pageSize = view === "grid" ? 12 : 10;
  const paged = useMemo(
    () => filtered.slice((page - 1) * pageSize, page * pageSize),
    [filtered, page, pageSize],
  );

  // Reset paging when the filter context changes.
  useEffect(() => {
    setPage(1);
  }, [tab, bucket, view]);

  /* ── Preview blob URL lifecycle ── */
  useEffect(() => {
    if (!preview) {
      setPreviewUrl(null);
      return;
    }
    let created: string | null = null;
    let revoked = false;
    resolveUploadedObjectUrl(preview)
      .then((u) => {
        if (revoked) {
          URL.revokeObjectURL(u);
          return;
        }
        created = u;
        setPreviewUrl(u);
      })
      .catch(() => undefined);
    return () => {
      revoked = true;
      if (created) URL.revokeObjectURL(created);
    };
  }, [preview]);

  async function handleUpload(file: File | undefined) {
    if (!file) return;
    try {
      await uploadMutation.mutateAsync(file);
    } catch {
      // toast already shown
    } finally {
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  async function handleDownload(file: FileInfoDto) {
    try {
      const blob = await fileCenterApi.downloadBlob(file.bucket, file.key);
      triggerDownload(blob, file.key);
    } catch {
      // toast already shown via mutation-style error surface
    }
  }

  async function handleDelete() {
    if (!deleting) return;
    try {
      await deleteMutation.mutateAsync({ bucket: deleting.bucket, key: deleting.key });
      setDeleting(null);
    } catch {
      // toast already shown
    }
  }

  async function handleBatchDelete() {
    const items = [...selectedKeys]
      .map((k) => files.find((f) => rowKeyOf(f) === k))
      .filter((f): f is FileInfoDto => !!f)
      .map((f) => ({ bucket: f.bucket, key: f.key }));
    if (items.length === 0) return;
    try {
      await batchDeleteMutation.mutateAsync(items);
    } catch {
      // toast already shown
    }
  }

  const csvColumns: CsvColumn<FileInfoDto>[] = [
    { title: t("system.files.fileName"), cell: (f) => f.key },
    { title: t("system.files.bucket"), cell: (f) => f.bucket },
    { title: t("common.type"), cell: (f) => f.contentType ?? "" },
    { title: t("common.size"), cell: (f) => f.size },
    {
      title: t("system.files.uploaded"),
      cell: (f) => new Date(f.lastModified).toLocaleString(),
    },
  ];

  const cols = [
    {
      key: "name",
      title: t("system.files.fileName"),
      render: (r: FileInfoDto) => (
        <Stack direction="row" spacing={1} sx={{ alignItems: "center", minWidth: 0 }}>
          {isImage(r) ? (
            <ImageIcon fontSize="small" color="action" />
          ) : isVideo(r) ? (
            <VideoIcon fontSize="small" color="action" />
          ) : (
            <UploadFileIcon fontSize="small" color="action" />
          )}
          <Typography variant="body2" sx={{ fontWeight: 500 }} noWrap>
            {r.key}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "bucket",
      title: t("system.files.bucket"),
      render: (r: FileInfoDto) => (
        <Box
          component="code"
          sx={{
            fontSize: 12,
            fontFamily: "monospace",
            bgcolor: "action.hover",
            px: 0.75,
            borderRadius: 1,
          }}
        >
          {r.bucket}
        </Box>
      ),
    },
    {
      key: "type",
      title: t("common.type"),
      render: (r: FileInfoDto) => (
        <Typography variant="caption" color="text.secondary">
          {r.contentType ?? "—"}
        </Typography>
      ),
    },
    { key: "size", title: t("common.size"), render: (r: FileInfoDto) => formatSize(r.size) },
    {
      key: "time",
      title: t("system.files.uploaded"),
      render: (r: FileInfoDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
          {new Date(r.lastModified).toLocaleString()}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: t("common.actions"),
      render: (r: FileInfoDto) => (
        <Stack direction="row" spacing={0.5}>
          {isImage(r) ? (
            <Button variant="text" size="sm" onClick={() => setPreview(r)}>
              {t("system.files.preview")}
            </Button>
          ) : null}
          <Button variant="text" size="sm" onClick={() => void handleDownload(r)}>
            {t("system.files.download")}
          </Button>
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => setDeleting(r)}>
              {t("common.delete")}
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("nav.files")}
        subtitle={t("system.files.uploadedFileManagement")}
        actions={
          <Stack direction="row" spacing={1}>
            <IconButton
              size="small"
              aria-label={view === "grid" ? t("system.files.listView") : t("system.files.gridView")}
              onClick={() => setView(view === "grid" ? "list" : "grid")}
            >
              {view === "grid" ? (
                <ViewListIcon fontSize="small" />
              ) : (
                <GridViewIcon fontSize="small" />
              )}
            </IconButton>
            <ExportCsvButton
              onClick={() => exportCsvFromColumns("files.csv", csvColumns, filtered)}
            />
            {canCreate ? (
              <Button variant="contained" onClick={() => setUploadOpen(true)}>
                {t("system.files.uploadFile")}
              </Button>
            ) : null}
          </Stack>
        }
      />

      <Box
        component="input"
        ref={inputRef}
        type="file"
        sx={{ display: "none" }}
        onChange={(e) => void handleUpload(e.target.files?.[0])}
      />

      <InfoBanner>
        {t("system.common.storageProvider")}{" "}
        <Typography component="strong" variant="body2" sx={{ fontWeight: 700, display: "inline" }}>
          Local
        </Typography>{" "}
        · {t("system.files.configureIn")}{" "}
        <Link component={RouterLink} to="/plugins/storage">
          Plugins → Storage
        </Link>
      </InfoBanner>

      <Box sx={{ display: "flex", gap: 2, alignItems: "flex-start" }}>
        <BucketTree buckets={buckets} bucket={bucket} onSelect={setBucket} />

        <Divider orientation="vertical" flexItem sx={{ display: { xs: "none", md: "block" } }} />

        {/* Main pane */}
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Tabs value={tab} onChange={(_, v) => setTab(v as MimeTab)} sx={{ mb: 2, minHeight: 40 }}>
            <Tab
              value="all"
              label={
                <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
                  <FolderIcon fontSize="small" />
                  {t("system.files.tab.all")}
                </Stack>
              }
              sx={{ minHeight: 40, textTransform: "none" }}
            />
            <Tab
              value="image"
              label={
                <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
                  <ImageIcon fontSize="small" />
                  {t("system.files.tab.image")}
                </Stack>
              }
              sx={{ minHeight: 40, textTransform: "none" }}
            />
            <Tab
              value="video"
              label={
                <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
                  <VideoIcon fontSize="small" />
                  {t("system.files.tab.video")}
                </Stack>
              }
              sx={{ minHeight: 40, textTransform: "none" }}
            />
          </Tabs>

          {view === "grid" ? (
            <>
              {paged.length === 0 && !loading ? (
                <Box sx={{ py: 8, textAlign: "center" }}>
                  <Typography variant="body2" color="text.secondary">
                    {t("common.noData")}
                  </Typography>
                </Box>
              ) : (
                <Grid container spacing={2}>
                  {paged.map((f) => (
                    <Grid key={rowKeyOf(f)} size={{ xs: 6, sm: 4, md: 3, lg: 2 }}>
                      <Box
                        sx={{
                          border: 1,
                          borderColor: "divider",
                          borderRadius: 2,
                          overflow: "hidden",
                        }}
                      >
                        {isImage(f) ? (
                          <ImageThumb file={f} onClick={() => setPreview(f)} />
                        ) : (
                          <Box
                            sx={{
                              width: "100%",
                              aspectRatio: "1 / 1",
                              bgcolor: "action.hover",
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                            }}
                          >
                            <VideoIcon color="disabled" sx={{ fontSize: 40 }} />
                          </Box>
                        )}
                        <Box sx={{ p: 1 }}>
                          <Typography variant="caption" noWrap sx={{ display: "block" }}>
                            {f.key}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {formatSize(f.size)}
                          </Typography>
                        </Box>
                      </Box>
                    </Grid>
                  ))}
                </Grid>
              )}
              <Box sx={{ display: "flex", justifyContent: "flex-end", mt: 2 }}>
                <Pagination
                  page={page}
                  pageSize={pageSize}
                  total={filtered.length}
                  onPage={setPage}
                  onPageSize={() => undefined}
                />
              </Box>
            </>
          ) : (
            <>
              <DataTable
                columns={cols}
                data={paged}
                loading={loading}
                rowKey={rowKeyOf}
                selectable={canDelete}
                selectedKeys={selectedKeys}
                onSelectedKeysChange={setSelectedKeys}
                tableId="files"
                batchActions={
                  <Button
                    variant="outlined"
                    color="danger"
                    size="sm"
                    disabled={batchDeleteMutation.isPending}
                    onClick={() => setBatchConfirmOpen(true)}
                  >
                    {t("common.batchDelete")}
                  </Button>
                }
                emptyAction={
                  canCreate ? (
                    <Button variant="contained" onClick={() => setUploadOpen(true)}>
                      {t("system.files.uploadFile")}
                    </Button>
                  ) : undefined
                }
              />
              <Box sx={{ display: "flex", justifyContent: "flex-end", mt: 2 }}>
                <Pagination
                  page={page}
                  pageSize={pageSize}
                  total={filtered.length}
                  onPage={setPage}
                  onPageSize={() => undefined}
                />
              </Box>
            </>
          )}
        </Box>
      </Box>

      <FilesDialogs
        uploadOpen={uploadOpen}
        onUploadClose={() => setUploadOpen(false)}
        onUploaded={reload}
        fileInputRef={inputRef}
        previewKey={preview?.key ?? null}
        previewUrl={previewUrl}
        onPreviewClose={() => setPreview(null)}
        deletingKey={deleting?.key ?? null}
        onDeleteClose={() => setDeleting(null)}
        onDeleteConfirm={() => void handleDelete()}
        batchOpen={batchConfirmOpen}
        onBatchClose={() => setBatchConfirmOpen(false)}
        onBatchConfirm={() => {
          setBatchConfirmOpen(false);
          void handleBatchDelete();
        }}
        batchPending={batchDeleteMutation.isPending}
        selectedCount={selectedKeys.size}
      />
    </PageStack>
  );
}

export default Files;
