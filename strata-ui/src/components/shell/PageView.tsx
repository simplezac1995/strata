// SPDX-License-Identifier: Apache-2.0
import { Outlet, useLocation } from "react-router-dom";
import { Box, Fade } from "@mui/material";
import { useApp } from "@/context/AppContext";

/** Main content outlet; key forces remount + fade-in on path or manual refresh. */
export function PageView() {
  const location = useLocation();
  const { pageRefreshKey } = useApp();
  const routeKey = `${location.pathname}${location.search}:${pageRefreshKey}`;
  return (
    <Box sx={{ flex: 1, minHeight: 0, display: "flex" }}>
      <Fade key={routeKey} in timeout={{ enter: 200, exit: 0 }}>
        <Box sx={{ flex: 1, minWidth: 0, minHeight: 0, display: "flex", flexDirection: "column" }}>
          <Outlet />
        </Box>
      </Fade>
    </Box>
  );
}
