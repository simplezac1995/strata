// SPDX-License-Identifier: Apache-2.0
import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Box,
  Chip,
  Dialog,
  IconButton,
  InputAdornment,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  TextField,
  Typography,
} from "@mui/material";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import CloseIcon from "@mui/icons-material/Close";
import HistoryIcon from "@mui/icons-material/History";
import SearchIcon from "@mui/icons-material/Search";
import StarIcon from "@mui/icons-material/Star";
import PeopleAltOutlinedIcon from "@mui/icons-material/PeopleAltOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import { useApp } from "@/context/AppContext";
import {
  NAV,
  deriveNavFromMenus,
  flattenNavEntries,
  openNavItem,
} from "@/components/shell/navigation";
import { useI18n } from "@/shared/i18n";
import { useAuthStore } from "@/shared/auth/store";
import { searchApi, type SearchHitDto } from "@/shared/api/search";

const RECENT_PATHS = ["/dashboard", "/system/users", "/ai/overview"];

export function SearchModal() {
  const { searchOpen, setSearchOpen, favoriteTabs, removeFavoriteTab } = useApp();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(0);
  const backendMenus = useAuthStore((s) => s.menus);
  const navItems = useMemo(
    () => flattenNavEntries(backendMenus.length ? deriveNavFromMenus(backendMenus) : NAV),
    [backendMenus],
  );

  useEffect(() => {
    if (searchOpen) {
      setQuery("");
      setSelected(0);
    }
  }, [searchOpen]);

  const q = query.trim().toLowerCase();

  // Debounce the business search so we don't fire a request on every keystroke.
  const [debouncedQuery, setDebouncedQuery] = useState("");
  useEffect(() => {
    const id = setTimeout(() => setDebouncedQuery(query.trim()), 300);
    return () => clearTimeout(id);
  }, [query]);

  const { data: business } = useQuery({
    queryKey: ["api", "search", "global", debouncedQuery],
    queryFn: () => searchApi.search(debouncedQuery, 10),
    enabled: debouncedQuery.length > 0,
  });

  const filtered = useMemo(() => {
    if (!q) return [];
    return navItems.filter((i) => {
      const label = t(i.label).toLowerCase();
      const section = t(i.section).toLowerCase();
      return label.includes(q) || section.includes(q);
    });
  }, [q, t, navItems]);

  const favoriteItems = useMemo(
    () =>
      favoriteTabs.map((tab) => ({
        path: tab.path,
        label: tab.label,
        icon: StarIcon,
        section: "shell.search.favorites",
      })),
    [favoriteTabs],
  );

  const recentItems = useMemo(
    () =>
      RECENT_PATHS.map((path) => {
        const hit = navItems.find((i) => i.path === path);
        if (!hit) return null;
        return { ...hit, icon: HistoryIcon, section: "shell.recent" };
      }).filter((x): x is NonNullable<typeof x> => x !== null),
    [navItems],
  );

  const items = useMemo(() => {
    if (q) return filtered;
    return [...favoriteItems, ...recentItems];
  }, [q, filtered, favoriteItems, recentItems]);

  useEffect(() => {
    setSelected(0);
  }, [query]);

  const go = (item: (typeof items)[number]) => {
    const path = openNavItem(item);
    if (path) navigate(path);
    setSearchOpen(false);
  };

  const goHit = (hit: SearchHitDto) => {
    navigate(hit.route);
    setSearchOpen(false);
  };

  const renderBusinessGroup = (
    title: string,
    hits: SearchHitDto[] | undefined,
    icon: React.ReactNode,
  ) => {
    if (!hits || hits.length === 0) return null;
    return (
      <Box key={title}>
        <Typography
          variant="overline"
          color="text.secondary"
          sx={{ px: 2, pt: 1, pb: 0.5, display: "block" }}
        >
          {title}
        </Typography>
        {hits.slice(0, 5).map((hit) => (
          <Box key={hit.id} sx={{ display: "flex", alignItems: "center", px: 1 }}>
            <ListItemButton onClick={() => goHit(hit)} sx={{ flex: 1, borderRadius: 1.5 }}>
              <ListItemIcon sx={{ minWidth: 36 }}>
                <Box
                  sx={{
                    width: 28,
                    height: 28,
                    borderRadius: 1.5,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    bgcolor: "action.hover",
                    color: "text.secondary",
                  }}
                >
                  {icon}
                </Box>
              </ListItemIcon>
              <ListItemText
                primary={hit.label}
                secondary={hit.subtitle ?? undefined}
                slotProps={{
                  primary: { variant: "body2", sx: { fontWeight: 500 } },
                  secondary: { variant: "caption" },
                }}
              />
              <ArrowForwardIcon sx={{ fontSize: 14, color: "text.disabled" }} />
            </ListItemButton>
          </Box>
        ))}
      </Box>
    );
  };

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setSelected((s) => Math.min(s + 1, items.length - 1));
    }
    if (e.key === "ArrowUp") {
      e.preventDefault();
      setSelected((s) => Math.max(s - 1, 0));
    }
    if (e.key === "Enter" && items[selected]) go(items[selected]);
    if (e.key === "Escape") setSearchOpen(false);
  };

  const footerHints = [
    { key: "\u2191\u2193", label: "shell.search.navigate" },
    { key: "Enter", label: "shell.search.open" },
    { key: "Esc", label: "shell.search.close" },
  ] as const;

  const renderItem = (
    item: (typeof items)[number],
    index: number,
    options?: { showRemoveFavorite?: boolean },
  ) => {
    const Icon = item.icon;
    const isSelected = index === selected;
    return (
      <Box key={`${item.path}-${index}`} sx={{ display: "flex", alignItems: "center", px: 1 }}>
        <ListItemButton
          selected={isSelected}
          onMouseEnter={() => setSelected(index)}
          onClick={() => go(item)}
          sx={{ flex: 1, borderRadius: 1.5 }}
        >
          <ListItemIcon sx={{ minWidth: 36 }}>
            <Box
              sx={{
                width: 28,
                height: 28,
                borderRadius: 1.5,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                bgcolor: isSelected ? "primary.light" : "action.hover",
                color: isSelected ? "primary.main" : "text.secondary",
              }}
            >
              <Icon sx={{ fontSize: 16 }} />
            </Box>
          </ListItemIcon>
          <ListItemText
            primary={t(item.label)}
            secondary={t(item.section)}
            slotProps={{
              primary: { variant: "body2", sx: { fontWeight: 500 } },
              secondary: { variant: "caption" },
            }}
          />
          <ArrowForwardIcon
            sx={{ fontSize: 14, color: isSelected ? "primary.main" : "text.disabled" }}
          />
        </ListItemButton>
        {options?.showRemoveFavorite ? (
          <IconButton
            size="small"
            aria-label={t("shell.favorite.remove")}
            onClick={() => removeFavoriteTab(item.path)}
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        ) : null}
      </Box>
    );
  };

  return (
    <Dialog
      open={searchOpen}
      onClose={() => setSearchOpen(false)}
      fullWidth
      maxWidth="sm"
      slotProps={{
        paper: {
          sx: { mt: "12vh", verticalAlign: "top", overflow: "hidden" },
        },
      }}
    >
      <Box sx={{ px: 2, py: 1.5, borderBottom: 1, borderColor: "divider" }}>
        <TextField
          autoFocus
          fullWidth
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKey}
          placeholder={t("shell.search.placeholder")}
          variant="standard"
          slotProps={{
            input: {
              disableUnderline: true,
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" color="action" />
                </InputAdornment>
              ),
              endAdornment: (
                <InputAdornment position="end">
                  {query ? (
                    <IconButton
                      size="small"
                      aria-label={t("common.clear")}
                      onClick={() => setQuery("")}
                    >
                      <CloseIcon fontSize="small" />
                    </IconButton>
                  ) : null}
                  <Chip
                    label="ESC"
                    size="small"
                    variant="outlined"
                    sx={{ display: { xs: "none", sm: "flex" } }}
                  />
                </InputAdornment>
              ),
            },
          }}
        />
      </Box>

      <Box sx={{ maxHeight: "60vh", overflow: "auto" }}>
        {items.length > 0 ? (
          <List disablePadding sx={{ py: 1 }}>
            {!q && favoriteItems.length > 0 ? (
              <Typography
                variant="overline"
                color="text.secondary"
                sx={{ px: 2, py: 0.5, display: "block" }}
              >
                {t("shell.search.favorites")}
              </Typography>
            ) : null}
            {!q
              ? favoriteItems.map((item, i) => renderItem(item, i, { showRemoveFavorite: true }))
              : null}
            {!q && recentItems.length > 0 ? (
              <Typography
                variant="overline"
                color="text.secondary"
                sx={{ px: 2, py: 0.5, display: "block" }}
              >
                {t("shell.search.recent")}
              </Typography>
            ) : null}
            {q ? (
              <Typography
                variant="overline"
                color="text.secondary"
                sx={{ px: 2, py: 0.5, display: "block" }}
              >
                {t("shell.search.resultCount").replace("{n}", String(filtered.length))}
              </Typography>
            ) : null}
            {(q ? items : recentItems).map((item, offset) => {
              const index = q ? offset : favoriteItems.length + offset;
              return renderItem(item, index);
            })}
          </List>
        ) : q ? (
          <Box sx={{ py: 6, textAlign: "center" }}>
            <SearchIcon sx={{ fontSize: 32, color: "text.disabled", mb: 1.5 }} />
            <Typography variant="body2" color="text.secondary">
              {t("shell.search.noResults")}{" "}
              <Typography component="span" variant="body2" sx={{ fontWeight: 600 }}>
                {query}
              </Typography>
            </Typography>
          </Box>
        ) : null}

        {q && business ? (
          <Box sx={{ pb: 1 }}>
            <Typography
              variant="overline"
              color="text.secondary"
              sx={{ px: 2, pt: 1, pb: 0.5, display: "block" }}
            >
              {t("shell.search.business")}
            </Typography>
            {renderBusinessGroup(
              t("shell.search.users"),
              business.users,
              <PeopleAltOutlinedIcon sx={{ fontSize: 16 }} />,
            )}
            {renderBusinessGroup(
              t("shell.search.roles"),
              business.roles,
              <ShieldOutlinedIcon sx={{ fontSize: 16 }} />,
            )}
            {renderBusinessGroup(
              t("shell.search.orders"),
              business.orders,
              <ReceiptLongOutlinedIcon sx={{ fontSize: 16 }} />,
            )}
          </Box>
        ) : null}
      </Box>

      <Box
        sx={{
          px: 2,
          py: 1.25,
          borderTop: 1,
          borderColor: "divider",
          display: "flex",
          gap: 2,
          flexWrap: "wrap",
        }}
      >
        {footerHints.map(({ key, label }) => (
          <Typography
            key={label}
            variant="caption"
            color="text.secondary"
            sx={{ display: "flex", gap: 0.75, alignItems: "center" }}
          >
            <Chip label={key} size="small" variant="outlined" sx={{ height: 20, fontSize: 10 }} />
            {t(label)}
          </Typography>
        ))}
      </Box>
    </Dialog>
  );
}
