// SPDX-License-Identifier: Apache-2.0
import { InspectionView } from "./InspectionView";
import { useInspection } from "./main/useInspection";

export function Inspection() {
  const view = useInspection();

  return <InspectionView view={view} />;
}
