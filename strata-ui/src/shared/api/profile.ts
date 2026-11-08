// SPDX-License-Identifier: Apache-2.0
import { apiGet, apiPut, toParams } from "@/shared/api/client";
import type { OperationLogDto, PageResult } from "@/shared/types";

/* ─── Current-user self-service profile (contract-aligned, do not guess) ── */

export const profileApi = {
  updateEmail: (email: string) => apiPut<void>("/api/v1/system/users/me/email", { email }),
  updatePhone: (phone: string) => apiPut<void>("/api/v1/system/users/me/phone", { phone }),
  updateAvatar: (avatar: string) => apiPut<void>("/api/v1/system/users/me/avatar", { avatar }),
  operationLogs: (page: number, size: number) =>
    apiGet<PageResult<OperationLogDto>>(
      `/api/v1/system/users/me/operation-logs${toParams({ page, size })}`,
    ),
};
