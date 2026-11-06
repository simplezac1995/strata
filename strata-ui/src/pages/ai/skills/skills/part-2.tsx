// SPDX-License-Identifier: Apache-2.0
import { SkillCrudView } from "./SkillCrudView";
import { useSkillCrud } from "./part-2/useSkillCrud";

export function SkillCrud({ type }: { type: "skill" | "interface" }) {
  const view = useSkillCrud({ type });

  return <SkillCrudView view={view} {...{ type }} />;
}
