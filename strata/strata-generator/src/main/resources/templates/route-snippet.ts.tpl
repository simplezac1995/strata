// ===== Manual insertion points for {{E}} CRUD =====
//
// 1. MENU: Paste menu-snippet.json into strata-system-biz/src/main/resources/seed/menus.json
//    under the appropriate parent directory's "children" array.
//    Add "nav.{{RESOURCE}}" to both locales in src/shared/i18n/resources/ (zh + en).
//
// 2. ROUTE TABLE: Append to src/routes/routeTable.tsx (under demoRoutes or appropriate):
//    { path: "{{RESOURCE}}", page: "{{E}}List", perm: ["{{PERM}}:list"] },
//
// 3. LAZY PAGES: Append to src/routes/lazyPages.ts:
//    const {{E}}List = lazy(() => import("@/pages/{{MODULE}}/{{E}}List"));
//    and add to the lazyPages export object:
//    {{E}}List,
//
// 4. PAGE: Create src/pages/{{MODULE}}/{{E}}List.tsx from frontend-page.tsx.tpl output.
//
