// SPDX-License-Identifier: Apache-2.0
import { describe, expect, it, vi } from "vitest";
import { render, waitFor } from "@testing-library/react";
import type { Editor } from "@tiptap/react";
import { RichTextEditor } from "./RichTextEditor";

describe("RichTextEditor", () => {
  it("mounts and renders the editable surface", () => {
    render(<RichTextEditor value="<p>initial</p>" onChange={() => {}} />);
    const surface = document.querySelector(".ProseMirror");
    expect(surface).toBeTruthy();
    expect(surface?.textContent).toContain("initial");
  });

  it("emits onChange with updated HTML when content changes", async () => {
    let editorRef: Editor | undefined;
    const onChange = vi.fn();

    render(
      <RichTextEditor
        value=""
        onChange={onChange}
        onReady={(e) => {
          editorRef = e;
        }}
      />,
    );

    await waitFor(() => expect(editorRef).toBeTruthy());
    editorRef?.chain().insertContent("<p>hello world</p>").run();

    await waitFor(() => expect(onChange).toHaveBeenCalled());
    const emitted = (onChange.mock.lastCall?.[0] ?? "") as string;
    expect(emitted).toContain("hello world");
  });

  it("exposes toolbar buttons with labelled actions", () => {
    render(<RichTextEditor value="" onChange={() => {}} />);
    expect(document.querySelector('[aria-label="Bold"], [aria-label="加粗"]')).toBeTruthy();
    expect(document.querySelector('[aria-label*="image" i], [aria-label*="图片"]')).toBeTruthy();
  });
});
