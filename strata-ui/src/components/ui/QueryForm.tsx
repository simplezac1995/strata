// SPDX-License-Identifier: Apache-2.0
import React, { useState, type ReactNode } from "react";
import {
  Box,
  Card as MuiCard,
  CardContent,
  Collapse,
  Drawer,
  Stack,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import { FilterList as FilterListIcon } from "@mui/icons-material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { useI18n } from "@/shared/i18n";
import { Button } from "./Button";
import {
  AdvancedFilterBuilder,
  type AdvancedCondition,
  type FilterFieldOption,
} from "./AdvancedFilterBuilder";

export const QUERY_FIELD_WIDTH = 200;

export function QueryField({
  children,
  width = QUERY_FIELD_WIDTH,
}: {
  children: ReactNode;
  width?: number | string;
}) {
  const fieldWidth = typeof width === "number" ? { xs: "100%", sm: width } : width;
  return <Box sx={{ width: fieldWidth, minWidth: 0, flex: "0 0 auto" }}>{children}</Box>;
}
QueryField.displayName = "QueryField";

function wrapQueryField(child: ReactNode): ReactNode {
  if (!React.isValidElement(child)) return child;
  if (child.type === QueryField) return child;
  return <QueryField>{child}</QueryField>;
}

export interface QueryFormAdvancedFilter {
  /** Fields available to the combined-condition builder. */
  fields: FilterFieldOption[];
  /** Stable key isolating saved templates in localStorage. */
  storageKey: string;
  /** Receives the current combined conditions on every change. */
  onChange: (conditions: AdvancedCondition[]) => void;
}

export function QueryForm({
  children,
  onSearch,
  onReset,
  advancedFilter,
}: {
  children: ReactNode;
  onSearch: () => void;
  onReset?: () => void;
  /** Optional "advanced filter" expansion; keeps the static form unchanged. */
  advancedFilter?: QueryFormAdvancedFilter;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const fields = React.Children.map(children, wrapQueryField);

  const submit = () => {
    setDrawerOpen(false);
    onSearch();
  };
  const reset = () => {
    onReset?.();
  };

  const actionButtons = (
    <>
      {advancedFilter ? (
        <Button
          type="button"
          variant="outlined"
          color="default"
          size="sm"
          onClick={() => setAdvancedOpen((v) => !v)}
          endIcon={
            <ExpandMoreIcon
              sx={{
                transform: advancedOpen ? "rotate(180deg)" : "none",
                transition: "transform .2s",
              }}
            />
          }
        >
          {t("filter.advanced")}
        </Button>
      ) : null}
      <Button
        type={isMobile ? "button" : "submit"}
        variant="contained"
        size="sm"
        onClick={isMobile ? submit : undefined}
      >
        {t("common.search")}
      </Button>
      {onReset ? (
        <Button variant="outlined" color="default" size="sm" onClick={reset}>
          {t("common.reset")}
        </Button>
      ) : null}
    </>
  );

  return (
    <MuiCard
      sx={{
        mb: 0,
        bgcolor: (theme) =>
          theme.palette.mode === "light" ? "rgba(255,255,255,0.92)" : "background.paper",
      }}
    >
      <CardContent sx={{ py: 2.5, "&:last-child": { pb: 2.5 } }}>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          {isMobile ? (
            <>
              <Button
                type="button"
                variant="outlined"
                fullWidth
                startIcon={<FilterListIcon />}
                onClick={() => setDrawerOpen(true)}
              >
                {t("common.filters")}
              </Button>
              <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}>
                <Box
                  sx={{
                    width: { xs: "100%", sm: 320 },
                    p: 2,
                    display: "flex",
                    flexDirection: "column",
                    gap: 2,
                  }}
                >
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    {t("common.filtersTitle")}
                  </Typography>
                  <Stack
                    spacing={2}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        submit();
                      }
                    }}
                  >
                    {fields}
                  </Stack>
                  <Stack direction="row" spacing={1} sx={{ pt: 1 }}>
                    {actionButtons}
                  </Stack>
                </Box>
              </Drawer>
            </>
          ) : (
            <Box
              sx={{
                display: "flex",
                flexWrap: "wrap",
                gap: 2,
                alignItems: "flex-end",
              }}
            >
              {fields}
              <Box
                sx={{
                  display: "flex",
                  gap: 1,
                  flexShrink: 0,
                  ml: { xs: 0, sm: "auto" },
                  width: { xs: "100%", sm: "auto" },
                  pt: { xs: 0.5, sm: 0 },
                }}
              >
                {actionButtons}
              </Box>
            </Box>
          )}
        </form>

        {advancedFilter ? (
          <Collapse in={advancedOpen} sx={{ pt: advancedOpen ? 2 : 0 }}>
            <Box
              sx={{
                borderTop: 1,
                borderColor: "divider",
                pt: 2,
                display: isMobile ? "none" : "block",
              }}
            >
              <AdvancedFilterBuilder
                fields={advancedFilter.fields}
                storageKey={advancedFilter.storageKey}
                onChange={advancedFilter.onChange}
              />
            </Box>
          </Collapse>
        ) : null}
      </CardContent>
    </MuiCard>
  );
}
