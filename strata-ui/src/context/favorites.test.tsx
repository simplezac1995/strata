// SPDX-License-Identifier: Apache-2.0
import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { AppProvider, useApp } from "@/context/AppContext";

const FAVORITES_STORAGE_KEY = "strata_favorite_tabs:v1";

function FavoriteProbe() {
  const { favoriteTab, favoriteTabs, removeFavoriteTab } = useApp();
  const tab = { id: "users", label: "nav.users", path: "/system/users" };

  return (
    <div>
      <button type="button" onClick={() => favoriteTab(tab)}>
        add-favorite
      </button>
      <button type="button" onClick={() => removeFavoriteTab(tab.path)}>
        remove-favorite
      </button>
      <span data-testid="favorite-count">{favoriteTabs.length}</span>
      <ul>
        {favoriteTabs.map((f) => (
          <li key={f.path}>{f.path}</li>
        ))}
      </ul>
    </div>
  );
}

describe("favorite tabs", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    cleanup();
  });

  it("adds and removes a favorite tab in localStorage", () => {
    render(
      <AppProvider>
        <FavoriteProbe />
      </AppProvider>,
    );

    expect(screen.getByTestId("favorite-count").textContent).toBe("0");

    fireEvent.click(screen.getByRole("button", { name: "add-favorite" }));
    expect(screen.getByTestId("favorite-count").textContent).toBe("1");
    expect(screen.getByText("/system/users")).toBeTruthy();

    const stored = JSON.parse(localStorage.getItem(FAVORITES_STORAGE_KEY) ?? "[]");
    expect(stored).toHaveLength(1);
    expect(stored[0].path).toBe("/system/users");

    fireEvent.click(screen.getByRole("button", { name: "remove-favorite" }));
    expect(screen.getByTestId("favorite-count").textContent).toBe("0");
    expect(localStorage.getItem(FAVORITES_STORAGE_KEY)).toBe("[]");
  });

  it("does not duplicate favorites for the same path", () => {
    render(
      <AppProvider>
        <FavoriteProbe />
      </AppProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "add-favorite" }));
    fireEvent.click(screen.getByRole("button", { name: "add-favorite" }));
    expect(screen.getByTestId("favorite-count").textContent).toBe("1");
  });
});
