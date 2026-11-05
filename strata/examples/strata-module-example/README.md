# strata-module-example

The golden path for adding a feature module to Strata, in one small jar. Copy this module, rename the package and
delete the note example; the build already treats it as a normal reactor member, so anything that works here works in
a real module.

## What it demonstrates

| Rule | Where |
| :--- | :--- |
| One capability = one Maven module | this `pom.xml` depends only on framework modules |
| Contract lives in `api` | `dev.strata.example.api.ExampleNote` / `ExampleNoteRequest` |
| Logic lives in `service`, not in the controller | `ExampleNoteService` |
| HTTP entry lives in `web` and returns `Result` | `ExampleNoteController` |
| The host application discovers the module automatically | `ExampleAutoConfiguration` + `META-INF/spring/...AutoConfiguration.imports` |
| A host can switch the module off without removing the jar | `@ConditionalOnProperty(prefix = "strata.example", name = "enabled")` |
| Tests need no database and no Spring context | `ExampleNoteServiceTest`, `ExampleNoteControllerTest` |

## Consume it from a host application

```xml
<dependency>
    <groupId>io.github.simplezac1995</groupId>
    <artifactId>strata-module-example</artifactId>
    <version>${strata.version}</version>
</dependency>
```

Spring Boot applies the auto-configuration at startup; `strata.example.enabled=false` disables it.

## Production checklist for a copy

1. Add the module to the root `pom.xml` (and to `strata-coverage/pom.xml` if you want it in the JaCoCo aggregate).
2. Replace the in-memory store with a repository + Flyway migration owned by your module.
3. Add permission constants to `Permissions` and `@PreAuthorize` to every write endpoint; register the authorities in
   the RBAC seed data.
4. Add the module's frontend route/menu by following `docs/17-结构约定.md` (the admin UI reads menus from the RBAC seed).
5. Keep each file under the 500-line cap enforced by `scripts/verify-file-size.mjs`.
