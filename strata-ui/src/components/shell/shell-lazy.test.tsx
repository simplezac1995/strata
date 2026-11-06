// SPDX-License-Identifier: Apache-2.0
import { Box } from "@mui/material";
import { describe, it, expect } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AppProvider } from "@/context/AppContext";
import { Sidebar } from "@/components/shell/Sidebar";
import { PageView } from "@/components/shell/PageView";
import { lazyPage } from "@/shared/pageKit/lazyPage";

const Dashboard = lazyPage(() => Promise.resolve({ default: () => <div>DASHBOARD_LAZY</div> }));

function Layout() {
  return (
    <Box sx={{ display: "flex", height: "100vh" }}>
      <Sidebar />
      <Box component="main" sx={{ flex: 1, minWidth: 0 }}>
        <PageView />
      </Box>
    </Box>
  );
}

describe("shell lazy outlet", () => {
  it("resolves async page inside single PageView outlet", async () => {
    render(
      <AppProvider>
        <MemoryRouter initialEntries={["/dashboard"]}>
          <Routes>
            <Route element={<Layout />}>
              <Route path="/dashboard" element={<Dashboard />} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AppProvider>,
    );

    expect(screen.getByText("加载中…")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByText("DASHBOARD_LAZY")).toBeTruthy();
    });
  });
});
