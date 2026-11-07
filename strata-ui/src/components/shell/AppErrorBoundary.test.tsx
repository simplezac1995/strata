// SPDX-License-Identifier: Apache-2.0
import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import { AppErrorBoundary } from "./AppErrorBoundary";
import { MemoryRouter } from "react-router-dom";

describe("AppErrorBoundary", () => {
  it("renders children normally when no error", () => {
    const { container } = render(
      <MemoryRouter>
        <AppErrorBoundary>
          <div data-testid="child">Normal content</div>
        </AppErrorBoundary>
      </MemoryRouter>,
    );
    expect(container.querySelector("[data-testid=child]")).toBeTruthy();
  });

  it("is a class component with getDerivedStateFromError", () => {
    expect(typeof AppErrorBoundary.getDerivedStateFromError).toBe("function");
  });

  it("has componentDidCatch method on prototype", () => {
    expect(typeof AppErrorBoundary.prototype.componentDidCatch).toBe("function");
  });
});
