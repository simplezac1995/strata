# Code generator tutorial

> The generator turns a field matrix into a full CRUD slice (backend + Flyway + admin console snippets). It is a
> development tool: **disabled by default and it cannot write under the `prod` profile**.

## 1. Add the module

The generator auto-configures itself when its jar is on the classpath:

```xml
<dependency>
  <groupId>io.github.simplezac1995</groupId>
  <artifactId>strata-generator</artifactId>
</dependency>
```

Enable it explicitly in non-production environments:

```yaml
strata:
  generator:
    enabled: true
```

## 2. Use it from the admin console

The reference console exposes the generator under **Tools → Code Generator** (`/tools/code-generator`). Provide:

- the target module (domain) and package,
- the entity/table name,
- the field matrix (name, type, nullable, searchable, list/detail visibility, validation),
- menu/permission options.

Submit **Preview** to inspect the files, then **Download** (zip) or **Write** (into the checkout when the server runs from
the project directory). `Write` is disabled under `prod`.

## 3. HTTP endpoints

| Method | Path | Purpose |
| :--- | :--- | :--- |
| POST | `/api/v1/generator/preview` | Render the file set without writing |
| POST | `/api/v1/generator/download` | Render the file set as a zip |
| POST | `/api/v1/generator/write` | Write to the checkout (dev only) |

All three are authority-protected and registered in the API contract; the UI uses the same endpoints.

## 4. What is generated

For a resource named `invoice` in module `billing`:

- backend: entity, DTO, mapper, repository, app service, controller, permission constants, demo seed and the Flyway
  migration;
- frontend: page component, API client, TypeScript types, route table entry, menu fragment and i18n keys.

## 5. Why the output is trustworthy

Three gates run in the repository CI (`node scripts/verify-codegen-templates.mjs`):

1. the generated Java is **really compiled**;
2. the generated frontend passes **`tsc`**;
3. the generated files pass **strict Prettier**.

If the templates drift from the codebase, the build fails — the generator cannot silently produce invalid code.

## 6. What still needs manual wiring (honest list)

The generator is a fast start, not a zero-touch scaffold: review and adjust

- business validation and error codes,
- menu/permission seeding for your roles,
- list/detail fields and column widths,
- integration tests for the new endpoint.

## 7. Security defaults

- Disabled by default; enable only in dev/test.
- `write` is blocked under `prod` and requires a filesystem path inside the running checkout.
- The generated endpoints follow the standard rules: `@PreAuthorize` with `module:entity:action`, RFC 7807 errors,
  pagination, audit and idempotency where applicable.
