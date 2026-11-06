// SPDX-License-Identifier: Apache-2.0
import { useCallback, useEffect, useState } from "react";
import {
  Box,
  DialogActions,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  ExportCsvButton,
  PageHeader,
  QueryForm,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { deptApi, type DeptPayload } from "@/shared/api/system";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { exportCsvFromColumns, type CsvColumn } from "@/shared/utils/csv";
import { toast } from "@/shared/pageKit/Toast";
import type { DeptDto } from "@/shared/types";

function updateDeptChildren(nodes: DeptDto[], parentId: string, children: DeptDto[]): DeptDto[] {
  return nodes.map((node) => {
    if (node.id === parentId) {
      return { ...node, children };
    }
    if (node.children.length > 0) {
      return { ...node, children: updateDeptChildren(node.children, parentId, children) };
    }
    return node;
  });
}

function DeptRow({
  dept,
  depth = 0,
  onEdit,
  onDelete,
  onExpand,
}: {
  dept: DeptDto;
  depth?: number;
  onEdit: (d: DeptDto) => void;
  onDelete: (d: DeptDto) => void;
  onExpand: (parentId: string) => Promise<void>;
}) {
  const { t } = useI18n();
  const [open, setOpen] = useState(depth === 0);
  const [loadingChildren, setLoadingChildren] = useState(false);
  const expandable = dept.children.length > 0 || depth < 3;

  const toggle = () => {
    if (!open && dept.children.length === 0) {
      setLoadingChildren(true);
      onExpand(dept.id).finally(() => {
        setLoadingChildren(false);
      });
    }
    setOpen((o) => !o);
  };

  const handleRowClick = () => {
    if (expandable) {
      toggle();
    }
  };

  return (
    <>
      <TableRow hover onClick={handleRowClick} sx={{ cursor: expandable ? "pointer" : "default" }}>
        <TableCell sx={{ pl: `${depth * 24 + 16}px` }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
            {expandable ? (
              <IconButton
                size="small"
                aria-label={open ? t("common.collapse") : t("common.expand")}
                onClick={(e) => {
                  e.stopPropagation();
                  void toggle();
                }}
                disabled={loadingChildren}
                sx={{ width: 16, height: 16, fontSize: 12 }}
              >
                {open ? "\u25BE" : "\u25B8"}
              </IconButton>
            ) : (
              <Box sx={{ width: 16 }} />
            )}
            <Typography variant="body2" sx={{ fontWeight: 500 }}>
              {dept.name}
            </Typography>
          </Stack>
        </TableCell>
        <TableCell>
          <Typography variant="body2" color="text.secondary">
            {dept.sortOrder}
          </Typography>
        </TableCell>
        <TableCell>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                bgcolor: dept.enabled ? "success.main" : "action.disabled",
              }}
            />
            <Typography variant="caption" color="text.secondary">
              {dept.enabled ? t("common.enabled") : t("common.disabled")}
            </Typography>
          </Stack>
        </TableCell>
        <TableCell>
          <Stack direction="row" spacing={0.5}>
            <Button variant="text" size="sm" onClick={() => onEdit(dept)}>
              {t("common.edit")}
            </Button>
            <Button variant="text" size="sm" color="danger" onClick={() => onDelete(dept)}>
              {t("common.delete")}
            </Button>
          </Stack>
        </TableCell>
      </TableRow>
      {open
        ? dept.children.map((child) => (
            <DeptRow
              key={child.id}
              dept={child}
              depth={depth + 1}
              onEdit={onEdit}
              onDelete={onDelete}
              onExpand={onExpand}
            />
          ))
        : null}
    </>
  );
}

function flatten(nodes: DeptDto[], depth = 0): { value: string; label: string }[] {
  const out: { value: string; label: string }[] = [];
  for (const node of nodes) {
    out.push({ value: node.id, label: "\u00A0".repeat(depth * 3) + node.name });
    if (node.children.length) out.push(...flatten(node.children, depth + 1));
  }
  return out;
}

export function Departments() {
  const { t } = useI18n();
  const canCreate = useAuthStore((s) => s.hasAuthority("system:dept:create"));
  const canUpdate = useAuthStore((s) => s.hasAuthority("system:dept:update"));
  const canDelete = useAuthStore((s) => s.hasAuthority("system:dept:delete"));

  const [depts, setDepts] = useState<DeptDto[]>([]);
  const [parentTree, setParentTree] = useState<DeptDto[]>([]);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const [roots, full] = await Promise.all([deptApi.children(), deptApi.list()]);
      setDepts(roots);
      setParentTree(full);
    } catch {
      toast.error("toast.loadFailed");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const expandNode = useCallback(async (parentId: string) => {
    try {
      const children = await deptApi.children(parentId);
      setDepts((prev) => updateDeptChildren(prev, parentId, children));
    } catch {
      toast.error("toast.loadFailed");
    }
  }, []);

  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [viewMode, setViewMode] = useState<"tree" | "flat">("tree");

  const {
    rows: flatDepts,
    total: flatTotal,
    page: flatPage,
    setPage: setFlatPage,
    pageSize: flatPageSize,
    setPageSize: setFlatPageSize,
    loading: flatLoading,
    reload: reloadFlat,
    error: flatError,
  } = usePagedQuery<DeptDto>(
    ["api", "system", "departments", "flat", { appliedKeyword }],
    (pageIndex, size) => deptApi.pageFlat(appliedKeyword || undefined, pageIndex, size),
  );

  function refreshAll() {
    void reload();
    if (viewMode === "flat") void reloadFlat();
  }

  const crud = useResourceCrud<
    DeptDto,
    DeptPayload,
    DeptPayload,
    { parentId: string; name: string; sortOrder: number; enabled: boolean }
  >({
    create: (payload) => deptApi.create(payload),
    update: (id, payload) => deptApi.update(id, payload),
    remove: (id) => deptApi.remove(id),
    reload: refreshAll,
    initForm: () => ({ parentId: "", name: "", sortOrder: 1, enabled: true }),
    toEditForm: (d) => ({
      parentId: d.parentId ?? "",
      name: d.name,
      sortOrder: d.sortOrder,
      enabled: d.enabled,
    }),
    toCreatePayload: (f) => ({
      parentId: f.parentId || null,
      name: f.name,
      sortOrder: f.sortOrder,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      parentId: f.parentId || null,
      name: f.name,
      sortOrder: f.sortOrder,
      enabled: f.enabled,
    }),
  });

  function openCreate(parentId: string | null = null) {
    crud.openCreate();
    crud.setForm((f) => ({ ...f, parentId: parentId ?? "" }));
  }

  const csvColumns: CsvColumn<DeptDto>[] = [
    { title: t("system.departments.departmentName"), cell: (d) => d.name },
    { title: t("common.sort"), cell: (d) => d.sortOrder },
  ];

  const parentOptions = [
    { value: "", label: t("system.departments.root") },
    ...flatten(parentTree).filter((o) => o.value !== crud.edit?.id),
  ];
  const headers = [
    t("system.departments.departmentName"),
    t("common.sort"),
    t("common.status"),
    t("common.actions"),
  ];

  return (
    <PageStack>
      <PageHeader
        title={t("nav.departments")}
        subtitle={t("system.departments.organizationalHierarchyManagement")}
        actions={
          <Stack direction="row" spacing={1}>
            <ExportCsvButton
              onClick={() => exportCsvFromColumns("departments.csv", csvColumns, flatDepts)}
            />
            {canCreate ? (
              <Button variant="contained" onClick={() => openCreate()}>
                {t("system.departments.createDepartment")}
              </Button>
            ) : null}
          </Stack>
        }
      />

      <QueryForm
        onSearch={() => {
          setAppliedKeyword(keyword.trim());
          setViewMode("flat");
          setFlatPage(1);
        }}
      >
        <TextField
          label={t("common.keyword")}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <Button
          variant="outlined"
          onClick={() => {
            setKeyword("");
            setAppliedKeyword("");
            setViewMode("tree");
          }}
        >
          {t("system.common.treeView")}
        </Button>
      </QueryForm>

      {viewMode === "flat" ? (
        <>
          <DataTable
            tableId="departments"
            columns={[
              { key: "name", title: t("system.departments.departmentName") },
              { key: "sortOrder", title: t("common.sort") },
            ]}
            data={flatDepts}
            loading={flatLoading}
            rowKey={(r) => r.id}
            error={flatError}
            onRetry={reloadFlat}
            emptyAction={
              canCreate ? (
                <Button variant="contained" onClick={() => openCreate()}>
                  {t("system.departments.createDepartment")}
                </Button>
              ) : undefined
            }
          />
          <Pagination
            page={flatPage}
            pageSize={flatPageSize}
            total={flatTotal}
            onPage={setFlatPage}
            onPageSize={setFlatPageSize}
          />
        </>
      ) : (
        <ContentCard padding={false}>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  {headers.map((h) => (
                    <TableCell key={h}>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ fontWeight: 700, textTransform: "uppercase" }}
                      >
                        {h}
                      </Typography>
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={4} align="center" sx={{ py: 8 }}>
                      <Typography color="text.secondary">{t("common.loading")}</Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  depts.map((d) => (
                    <DeptRow
                      key={d.id}
                      dept={d}
                      onEdit={canUpdate ? crud.openEdit : () => {}}
                      onDelete={canDelete ? crud.confirmDelete : () => {}}
                      onExpand={expandNode}
                    />
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </ContentCard>
      )}

      <Dialog
        open={crud.open}
        onClose={crud.closeDialog}
        title={t(
          crud.edit ? "system.departments.editDepartment" : "system.departments.createDepartment2",
        )}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Select
            label={t("system.departments.parentDepartment")}
            value={crud.form.parentId}
            onChange={(e) => crud.setForm((f) => ({ ...f, parentId: e.target.value }))}
            options={parentOptions}
          />
          <TextField
            label={t("common.nameRequired")}
            placeholder={t("system.departments.placeholderDepartment")}
            value={crud.form.name}
            onChange={(e) => crud.setForm((f) => ({ ...f, name: e.target.value }))}
          />
          <TextField
            label={t("common.sortOrder")}
            type="number"
            placeholder="1"
            value={String(crud.form.sortOrder)}
            onChange={(e) => crud.setForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))}
          />
          <Select
            label={t("common.status")}
            value={crud.form.enabled ? "true" : "false"}
            onChange={(e) => crud.setForm((f) => ({ ...f, enabled: e.target.value === "true" }))}
            options={[
              { value: "true", label: t("common.enabled") },
              { value: "false", label: t("common.disabled") },
            ]}
          />
        </Stack>
        <DialogActions sx={{ px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={crud.closeDialog}>
            {t("common.cancel")}
          </Button>
          <Button variant="contained" disabled={crud.busy} onClick={() => void crud.handleSave()}>
            {crud.busy ? t("common.saving") : t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={crud.deleting !== null}
        onClose={crud.closeDelete}
        onConfirm={() => void crud.runDelete()}
        title={t("system.departments.deleteDepartment")}
        description={
          crud.deleting && crud.deleting.children.length > 0
            ? t("system.departments.cannotDeleteThisDepartmentHasSubDepartment")
            : t("system.departments.delete.withAsterisk").replace(
                "{name}",
                crud.deleting?.name ?? "",
              )
        }
      />
    </PageStack>
  );
}

export default Departments;
