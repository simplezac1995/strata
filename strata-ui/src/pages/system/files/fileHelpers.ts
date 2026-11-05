// SPDX-License-Identifier: Apache-2.0
import type { FileInfoDto } from "@/shared/types";

/** Formats a byte count for the file list. Also used by tests and other storage pages. */
export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const units = ["KB", "MB", "GB"];
  let value = bytes / 1024;
  let unit = units[0];
  for (let i = 1; i < units.length && value >= 1024; i += 1) {
    value /= 1024;
    unit = units[i];
  }
  return `${value.toFixed(1)} ${unit}`;
}

export function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export type MimeTab = "all" | "image" | "video";
export type ViewMode = "grid" | "list";

export const ALL_BUCKET = "__all__";

export function isImage(file: FileInfoDto) {
  return (file.contentType ?? "").startsWith("image/");
}

export function isVideo(file: FileInfoDto) {
  return (file.contentType ?? "").startsWith("video/");
}
