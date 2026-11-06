# Admin console

The React admin console for Strata: React 19, TypeScript 6, MUI 9, Vite 8, TanStack Query 5, Zustand 5,
React Router 7. Package manager: pnpm 11 (`packageManager` is pinned in `package.json`).

## Quick start

```bash
pnpm install
pnpm dev          # http://localhost:5173, proxies /api and /actuator to :8080
```

## Commands

| Command                                      | Purpose                                                                                     |
| :------------------------------------------- | :------------------------------------------------------------------------------------------ |
| `pnpm dev`                                   | Vite dev server                                                                             |
| `pnpm build` / `pnpm preview`                | type-checked production build / preview                                                     |
| `pnpm check`                                 | full gate: typecheck + lint + unused scan + tests + i18n + formatting + build + bundle size |
| `pnpm test` / `pnpm typecheck` / `pnpm lint` | individual gates                                                                            |
| `pnpm i18n:verify` / `pnpm i18n:scan`        | dictionary parity / hard-coded strings                                                      |
| `pnpm e2e` / `pnpm e2e:install`              | Playwright (the open-source minimal app uses `playwright.minimal.config.ts`)                |
| `pnpm analyze`                               | bundle visualisation                                                                        |

## Structure

```
src/
├── pages/<domain>/        # feature pages (PascalCase files, kebab-case routes)
├── components/ui/         # design-system wrappers (Button, DataTable, Dialog, QueryForm, chart, ...)
├── components/shell/      # layout shell, sidebar, breadcrumbs, error boundary
├── routes/                # route table, lazy page registry, guards
├── shared/                # api clients, auth, query, hooks, i18n, websocket, monitoring, performance
├── theme/ · context/
└── main.tsx · App.tsx
```

Conventions:

- Server state lives in TanStack Query; local UI state in Zustand; no server data cached in Zustand.
- All visible text uses `t("domain.key")` semantic keys; dictionaries are `shared/i18n/` (zh/en) and the i18n
  gates fail on missing keys or hard-coded strings.
- Permissions use the backend authority strings (`module:entity:action`); route guards render `<Perm>`.
- Pages are lazy-loaded; `manualChunks` splits vendor code; the single-chunk budget is 600 kB and the build gate
  enforces it.
- Accessibility: accessible names on interactive controls, axe-core smoke tests for shared primitives, error boundary
  around routed content.
- Browser support: Chrome/Edge ≥ 90, Firefox ≥ 90, Safari ≥ 14; build target ES2022.

## Performance budgets

| Metric             | Budget   |
| :----------------- | :------- |
| Initial JS (gzip)  | ≤ 300 kB |
| Single chunk       | ≤ 600 kB |
| LCP                | ≤ 2.5 s  |
| CLS                | ≤ 0.1    |
| Initial CSS (gzip) | ≤ 150 kB |

`pnpm analyze` visualises the bundle; the build gate fails any chunk above 600 kB.

## Environment variables

Copy `.env.example` to `.env.local` and adjust as needed:

| Variable                                    | Default         | Purpose                                                     |
| :------------------------------------------ | :-------------- | :---------------------------------------------------------- |
| `VITE_DEFAULT_LANG`                         | `zh`            | initial language (`zh`/`en`), user choice wins              |
| `VITE_API_BASE_URL`                         | empty           | backend base URL; empty uses the dev proxy                  |
| `VITE_API_ENCRYPT` / `VITE_API_ENCRYPT_KEY` | `false` / empty | optional AES-GCM request encryption, must match the backend |
