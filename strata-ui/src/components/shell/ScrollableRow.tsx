// SPDX-License-Identifier: Apache-2.0
import { useRef, useState, useEffect, useCallback, type ReactNode } from "react";
import { Box, IconButton, useTheme } from "@mui/material";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import { useI18n } from "@/shared/i18n";
import { scrollButtonSx } from "@/theme/shellStyles";

interface ScrollableRowProps {
  children: ReactNode;
  sx?: Record<string, unknown>;
  innerSx?: Record<string, unknown>;
  gradient?: boolean;
  scrollIntoViewSelector?: string | null;
}

/** Horizontal strip with prev/next arrows when content overflows. */
export function ScrollableRow({
  children,
  sx,
  innerSx,
  gradient = false,
  scrollIntoViewSelector,
}: ScrollableRowProps) {
  const { t } = useI18n();
  const theme = useTheme();
  const ref = useRef<HTMLDivElement>(null);
  const [edge, setEdge] = useState({ left: false, right: false });

  const refresh = useCallback(() => {
    const el = ref.current;
    if (!el) return;
    const max = el.scrollWidth - el.clientWidth;
    const left = el.scrollLeft > 2;
    const right = max > 2 && el.scrollLeft < max - 2;
    setEdge((prev) => (prev.left === left && prev.right === right ? prev : { left, right }));
  }, []);

  useEffect(() => {
    refresh();
    const el = ref.current;
    if (!el) return;
    el.addEventListener("scroll", refresh, { passive: true });
    const ro = new ResizeObserver(refresh);
    ro.observe(el);
    return () => {
      el.removeEventListener("scroll", refresh);
      ro.disconnect();
    };
  }, [refresh]);

  useEffect(() => {
    if (!scrollIntoViewSelector || !ref.current) return;
    const target = ref.current.querySelector(scrollIntoViewSelector);
    target?.scrollIntoView({ inline: "nearest", block: "nearest", behavior: "smooth" });
    const timer = window.setTimeout(refresh, 300);
    return () => window.clearTimeout(timer);
  }, [scrollIntoViewSelector, refresh]);

  const scroll = (dir: -1 | 1) => {
    ref.current?.scrollBy({ left: dir * 200, behavior: "smooth" });
  };

  const btnSx = scrollButtonSx(gradient, theme);

  return (
    <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, minWidth: 0, ...sx }}>
      {edge.left ? (
        <IconButton
          size="small"
          onClick={() => scroll(-1)}
          aria-label={t("shell.scrollLeft")}
          sx={btnSx}
        >
          <ChevronLeftIcon sx={{ fontSize: 16 }} />
        </IconButton>
      ) : null}
      <Box
        ref={ref}
        sx={{
          flex: 1,
          overflowX: "auto",
          minWidth: 0,
          display: "flex",
          alignItems: "center",
          gap: 0.5,
          scrollbarWidth: "none",
          "&::-webkit-scrollbar": { display: "none" },
          ...innerSx,
        }}
      >
        {children}
      </Box>
      {edge.right ? (
        <IconButton
          size="small"
          onClick={() => scroll(1)}
          aria-label={t("shell.scrollRight")}
          sx={btnSx}
        >
          <ChevronRightIcon sx={{ fontSize: 16 }} />
        </IconButton>
      ) : null}
    </Box>
  );
}
