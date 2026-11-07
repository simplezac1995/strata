# FAQ

## What is Strata?

A modular-monolith backend framework for admin products: RBAC, operations tooling, notifications, SSO (OIDC/SAML),
SCIM provisioning, an optional AI domain, a plugin SPI, and a React admin console — one deployable Spring Boot
application. Module boundaries are enforced by Maven modules, ArchUnit rules and a Spring Modulith module-graph gate
that run in public CI.

## Is this related to OpenGamma's Strata?

No. OpenGamma's [Strata](https://github.com/OpenGamma/Strata) is a market-risk/pricing library. This project is an
unrelated modular-monolith admin framework; the shared word is a dictionary collision.

## Which Java version do I need?

Build with **JDK 25** (the only build toolchain); the published artifacts target **Java 21 bytecode**, so applications
can run on Java 21+. CI starts the built minimal application on a Java 21 runtime on every push.

## Where can I try it?

Hosted evaluation instance: <https://demo.simplecharlie.dev> (`admin` / `admin123`; shared, data resets periodically —
do not store anything real). Locally: `git clone … && docker compose up --build` → <http://localhost:3001>.

## Why a modular monolith instead of microservices?

Admin/ops products rarely need distributed transactions or independent scaling per domain. A single deployable with
enforced seams keeps the operational surface small while still allowing a module to be extracted later. There is no
service discovery, gateway, Feign or K8s-first deployment in the supported surface.

## Do I have to use the shipped admin console?

No. The backend is framework-first; the React admin console is included as a reference UI. Every resource is a plain
REST endpoint with OpenAPI documentation, so any frontend (or another admin template) can consume it.

## Why MUI rather than Tailwind / shadcn?

The admin console prioritises dense, data-heavy screens and accessibility-tested primitives; MUI 9 is the single UI
dependency and is wrapped behind `components/ui` so it can be replaced page by page. The backend framework makes no UI
choice for you.

## Is there an enterprise edition?

Community is Apache-2.0: the framework, starter, plugin SPI, admin console and examples in this repository. Business
verticals (the private product, compliance/KYC/AML and payment surfaces) are not part of this repository and no SaaS
lock-in is implied. Editions are documented in the root README.

## Can I use it commercially?

Yes, under Apache License 2.0. Keep the NOTICE file and the licence notices. No contributor licence agreement is
required; contributions use a DCO sign-off.

## How do I contribute?

See [CONTRIBUTING.md](../CONTRIBUTING.md): `mvn -B verify` (JDK 25) plus `pnpm check` for the console, conventional
commits and a DCO sign-off. Documentation and issue reports count as contributions.

## Where do I ask questions?

GitHub Issues for bugs/features and `good first issue` tasks; security reports go through the private vulnerability
reporting described in [SECURITY.md](../SECURITY.md). Response times are best-effort (single maintainer, see
[MAINTAINERS.md](../MAINTAINERS.md)).
