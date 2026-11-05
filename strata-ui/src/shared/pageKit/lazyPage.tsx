// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState, type ComponentType } from "react";
import { Box, Button, Skeleton, Stack, Typography } from "@mui/material";
import { useI18n } from "@/shared/i18n";
import { error as logError } from "@/shared/logger";

type LoadState = "loading" | "error" | ComponentType;

/**
 * Route-level first-screen skeleton shown while a lazy page chunk loads.
 *
 * <p>Mirrors the typical CRUD {@code PageStack} shape (header row + query/filter card +
 * content card) so switching routes never flashes a blank content area. The
 * {@code common.loading} text is kept screen-reader-only for accessibility and
 * automated checks; the visual affordance is the skeleton itself.
 */
export function PageFallback() {
  const { t } = useI18n();
  return (
    <Box role="status" aria-busy="true" sx={{ width: "100%" }}>
      <Typography
        sx={{
          position: "absolute",
          width: 1,
          height: 1,
          p: 0,
          m: -1,
          overflow: "hidden",
          clip: "rect(0 0 0 0)",
          whiteSpace: "nowrap",
          border: 0,
        }}
      >
        {t("common.loading")}
      </Typography>
      <Stack
        direction="row"
        sx={{ mb: 2.5, alignItems: "center", justifyContent: "space-between" }}
      >
        <Skeleton variant="text" width={180} height={32} />
        <Skeleton variant="rounded" width={120} height={36} />
      </Stack>
      <Skeleton variant="rounded" width="100%" height={72} sx={{ mb: 2 }} />
      <Skeleton variant="rounded" width="100%" height={360} />
    </Box>
  );
}

function PageLoadError({ onRetry }: { onRetry: () => void }) {
  const { t } = useI18n();
  return (
    <Box
      sx={{
        minHeight: "40vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <Stack spacing={1.5} direction="column" sx={{ alignItems: "center" }}>
        <Typography variant="body2" color="error">
          {t("error.page.load.failed")}
        </Typography>
        <Button onClick={onRetry}>{t("common.retry")}</Button>
      </Stack>
    </Box>
  );
}

/** Lazy route component with explicit loading/error (no Suspense). */
export function lazyPage(importer: () => Promise<{ default: ComponentType }>): ComponentType {
  return function LazyPage() {
    const [state, setState] = useState<LoadState>("loading");
    const [retryCount, setRetryCount] = useState(0);

    useEffect(() => {
      let alive = true;
      setState("loading");
      importer()
        .then((m) => {
          if (!alive) return;
          setState(() => m.default);
        })
        .catch((err) => {
          if (!alive) return;
          logError("[lazyPage] failed to load chunk", err);
          setState("error");
        });
      return () => {
        alive = false;
      };
    }, [retryCount]);

    if (state === "loading") return <PageFallback />;
    if (state === "error") return <PageLoadError onRetry={() => setRetryCount((c) => c + 1)} />;
    const Comp = state;
    return <Comp />;
  };
}

/** Lazy-load a named export from a page module. The module may also export
 *  non-route helpers (e.g. prop-taking subcomponents); only `name` is used. */
export function namedPage<T extends Record<string, unknown>>(
  importer: () => Promise<T>,
  name: keyof T & string,
): ComponentType {
  return function NamedLazyPage() {
    const [state, setState] = useState<LoadState>("loading");
    const [retryCount, setRetryCount] = useState(0);

    useEffect(() => {
      let alive = true;
      setState("loading");
      importer()
        .then((m) => {
          if (!alive) return;
          const Comp = m[name] as ComponentType | undefined;
          if (!Comp) {
            logError(`[lazyPage] named export "${name}" missing from chunk`);
            setState("error");
            return;
          }
          setState(() => Comp);
        })
        .catch((err) => {
          if (!alive) return;
          logError("[lazyPage] failed to load named chunk", err);
          setState("error");
        });
      return () => {
        alive = false;
      };
    }, [name, retryCount]); // eslint-disable-line react-hooks/exhaustive-deps

    if (state === "loading") return <PageFallback />;
    if (state === "error") return <PageLoadError onRetry={() => setRetryCount((c) => c + 1)} />;
    const Comp = state;
    return <Comp />;
  };
}
