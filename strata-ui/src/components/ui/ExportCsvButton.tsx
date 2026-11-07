// SPDX-License-Identifier: Apache-2.0
import { Button } from "./Button";
import { useI18n } from "@/shared/i18n";

export function ExportCsvButton({
  onClick,
  disabled,
}: {
  onClick: () => void;
  disabled?: boolean;
}) {
  const { t } = useI18n();
  return (
    <Button variant="outlined" onClick={onClick} disabled={disabled}>
      {t("common.exportCsv")}
    </Button>
  );
}
