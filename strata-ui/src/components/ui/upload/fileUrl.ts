// SPDX-License-Identifier: Apache-2.0
import type { FileInfoDto } from "@/shared/types";
import { fileApi } from "@/shared/api/system";

/**
 * Build a browser-usable object URL for an already-uploaded file.
 *
 * The backend download endpoint (`GET /api/v1/system/files/{bucket}/{key}/download`)
 * is auth-gated (`system:file:list`) and returns `Content-Disposition: attachment`
 * with `application/octet-stream`, so it cannot be referenced directly from an
 * `<img src>` tag. We fetch the bytes through the authenticated axios client and
 * hand back a `URL.createObjectURL` result.
 *
 * Callers are responsible for `URL.revokeObjectURL(url)` when they no longer need
 * it. Note: this URL is session-scoped — durable rendering of stored HTML/blog body
 * still needs a public inline image endpoint on the backend.
 */
export async function resolveUploadedObjectUrl(dto: FileInfoDto): Promise<string> {
  const blob = await fileApi.download(dto.bucket, dto.key);
  return URL.createObjectURL(blob);
}

/** Read a local File into an object URL (no network). */
export function fileToObjectUrl(file: Blob): string {
  return URL.createObjectURL(file);
}
