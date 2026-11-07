// SPDX-License-Identifier: Apache-2.0
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button, TextField, DataTable, Dialog } from "@/components/ui/index";

type Row = { id: string; name: string };

describe("component smoke", () => {
  it("renders a Button with its accessible name", () => {
    render(<Button>Save</Button>);
    expect(screen.getByRole("button", { name: "Save" })).toBeTruthy();
  });

  it("renders a TextField label and input", () => {
    render(<TextField label="Email" />);
    expect(screen.getByRole("textbox", { name: "Email" })).toBeTruthy();
  });

  it("renders an empty DataTable with header and empty state", () => {
    render(
      <DataTable
        columns={[{ key: "name", title: "Name" }]}
        data={[] as Row[]}
        rowKey={(r) => r.id}
      />,
    );
    expect(screen.getByText("Name")).toBeTruthy();
    expect(screen.getByText("暂无数据")).toBeTruthy();
  });

  it("renders an open Dialog with title and body", () => {
    render(
      <Dialog open onClose={() => {}} title="Confirm">
        <p>Dialog body</p>
      </Dialog>,
    );
    expect(screen.getByText("Confirm")).toBeTruthy();
    expect(screen.getByText("Dialog body")).toBeTruthy();
  });
});
