// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { Stack } from "@mui/material";
import { PageHeader } from "@/components/ui/index";
import { AiBadge } from "@/components/ui/index";

/** AI workspace page header — PageHeader + AiBadge. */
export function AiPageHeader({
  title,
  subtitle,
  actions,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <PageHeader
      title={
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <span>{title}</span>
          <AiBadge />
        </Stack>
      }
      subtitle={subtitle}
      actions={actions}
    />
  );
}
