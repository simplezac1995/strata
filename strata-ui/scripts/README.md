# scripts/

| Script                   | npm command        | Purpose                                                  |
| :----------------------- | :----------------- | :------------------------------------------------------- |
| `check.mjs`              | `pnpm check`       | Quality gate orchestration                               |
| `verify-i18n.mjs`        | `pnpm i18n:verify` | Ensure every `t()` / literal short key exists in `zh.ts` |
| `scan-hardcoded-ui.mjs`  | `pnpm i18n:scan`   | Detect hardcoded UI strings                              |
| `patch-zh-overrides.mjs` | `pnpm i18n:patch`  | Merge `zh-overrides.json` into dictionaries              |

One-off i18n migration scripts → `archive/i18n-migration/`（含原 `clean-i18n-dict.mjs` 等，仅参考）。

**Adding translations:** edit `src/shared/i18n/zh.ts` and `en.ts`, or add entries to `zh-overrides.json` then run `pnpm i18n:patch`. Use semantic domains — not `text.*`.

`hardcoded-ui-scan.json` is scan output (gitignored).
