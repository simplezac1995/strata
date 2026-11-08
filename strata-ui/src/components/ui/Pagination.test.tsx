// SPDX-License-Identifier: Apache-2.0
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { Pagination } from "./Pagination";

vi.mock("@/shared/i18n", () => ({
  useI18n: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        "common.totalRecords": "共 {n} 条",
        "common.pageSize": "{s} 条/页",
      };
      return map[key] || key;
    },
  }),
}));

describe("Pagination", () => {
  const onPage = vi.fn();
  const onPageSize = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders total records text", () => {
    render(
      <Pagination total={100} page={1} pageSize={10} onPage={onPage} onPageSize={onPageSize} />,
    );
    expect(screen.getByText("共 100 条")).toBeTruthy();
  });

  it("renders pagination nav element", () => {
    const { container } = render(
      <Pagination total={100} page={1} pageSize={10} onPage={onPage} onPageSize={onPageSize} />,
    );
    expect(container.querySelector("nav")).toBeTruthy();
  });

  it("renders page size select", () => {
    const { container } = render(
      <Pagination total={100} page={1} pageSize={10} onPage={onPage} onPageSize={onPageSize} />,
    );
    expect(container.querySelector(".MuiSelect-root")).toBeTruthy();
  });

  it("renders with different total values", () => {
    render(
      <Pagination total={42} page={1} pageSize={10} onPage={onPage} onPageSize={onPageSize} />,
    );
    expect(screen.getByText("共 42 条")).toBeTruthy();
  });

  it("renders with zero total", () => {
    render(<Pagination total={0} page={1} pageSize={10} onPage={onPage} onPageSize={onPageSize} />);
    expect(screen.getByText("共 0 条")).toBeTruthy();
  });
});
