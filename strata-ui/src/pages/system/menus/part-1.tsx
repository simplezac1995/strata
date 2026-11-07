// SPDX-License-Identifier: Apache-2.0
// Split out of Menus.tsx by scripts/auto-split.mjs.

import { useState } from "react";
import { Box, IconButton, Stack, TableCell, TableRow, Typography } from "@mui/material";
import { Button, Chip } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import type { MenuDto, MenuType } from "@/shared/types";

export const typeChipColor: Record<string, string> = {
  directory: "warning",
  menu: "info",
  button: "secondary",
};

export function updateTreeChildren(
  nodes: MenuDto[],
  parentId: string,
  children: MenuDto[],
): MenuDto[] {
  return nodes.map((node) => {
    if (node.id === parentId) {
      return { ...node, children };
    }
    if (node.children.length > 0) {
      return { ...node, children: updateTreeChildren(node.children, parentId, children) };
    }
    return node;
  });
}

export function MenuRow({
  item,
  depth = 0,
  onEdit,
  onAddChild,
  onDelete,
  onExpand,
}: {
  item: MenuDto;
  depth?: number;
  onEdit: (i: MenuDto) => void;
  onAddChild: (i: MenuDto) => void;
  onDelete: (i: MenuDto) => void;
  onExpand: (parentId: string) => Promise<void>;
}) {
  const { t } = useI18n();
  const [open, setOpen] = useState(depth < 1);
  const [loadingChildren, setLoadingChildren] = useState(false);
  const expandable = item.type === "directory" || item.children.length > 0;

  const toggle = () => {
    if (!open && expandable && item.children.length === 0) {
      setLoadingChildren(true);
      onExpand(item.id).finally(() => {
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
              <Box sx={{ width: 16, flexShrink: 0 }} />
            )}
            <Typography variant="body2" sx={{ fontWeight: 500 }}>
              {t(item.name)}
            </Typography>
          </Stack>
        </TableCell>
        <TableCell>
          <Chip label={item.type} color={typeChipColor[item.type] ?? "default"} />
        </TableCell>
        <TableCell>
          <Typography variant="caption" sx={{ fontFamily: "monospace" }} color="text.secondary">
            {item.path || "—"}
          </Typography>
        </TableCell>
        <TableCell>
          <Typography variant="caption" color="text.secondary">
            {item.permission || "—"}
          </Typography>
        </TableCell>
        <TableCell>
          <Typography variant="body2" color="text.secondary">
            {item.sortOrder}
          </Typography>
        </TableCell>
        <TableCell>
          <Box
            sx={{
              width: 8,
              height: 8,
              borderRadius: "50%",
              bgcolor: item.enabled ? "success.main" : "action.disabled",
              display: "inline-block",
            }}
          />
        </TableCell>
        <TableCell>
          <Stack direction="row" spacing={0.5}>
            {item.type !== "button" && (
              <Button variant="text" size="sm" color="primary" onClick={() => onAddChild(item)}>
                {t("system.menus.child")}
              </Button>
            )}
            <Button variant="text" size="sm" onClick={() => onEdit(item)}>
              {t("common.edit")}
            </Button>
            <Button variant="text" size="sm" color="danger" onClick={() => onDelete(item)}>
              {t("common.delete")}
            </Button>
          </Stack>
        </TableCell>
      </TableRow>
      {open
        ? item.children.map((child) => (
            <MenuRow
              key={child.id}
              item={child}
              depth={depth + 1}
              onEdit={onEdit}
              onAddChild={onAddChild}
              onDelete={onDelete}
              onExpand={onExpand}
            />
          ))
        : null}
    </>
  );
}

export function flatten(
  nodes: MenuDto[],
  t: (k: string) => string,
  depth = 0,
): { value: string; label: string }[] {
  const out: { value: string; label: string }[] = [];
  for (const node of nodes) {
    out.push({ value: node.id, label: "\u00A0".repeat(depth * 3) + t(node.name) });
    if (node.children.length) out.push(...flatten(node.children, t, depth + 1));
  }
  return out;
}

export const EMPTY_FORM = {
  parentId: "",
  name: "",
  type: "menu" as MenuType,
  path: "",
  component: "",
  permission: "",
  sortOrder: 1,
  visible: true,
  enabled: true,
};
