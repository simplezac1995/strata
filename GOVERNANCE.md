# Governance

> Strata is maintainer-driven; this page states how decisions and releases happen so consumers and contributors know
> what to expect.

## Roles

- **Maintainer** — currently [@simplezac1995](https://github.com/simplezac1995); merges changes, publishes releases,
  handles security reports, owns the roadmap.
- **Contributors** — anyone submitting issues, documentation or code under the DCO; reviews are best-effort.

See [MAINTAINERS.md](MAINTAINERS.md) for ownership and response expectations.

## Decision making

- Small changes (docs, fixes, tests) are decided by PR review under lazy consensus.
- Architectural changes get an ADR in the private product repository; consumer-visible decisions are summarised in
  [docs/decisions.md](docs/decisions.md).
- Breaking changes follow the policy in [docs/upgrade-guide.md](docs/upgrade-guide.md).
- Roadmap items live in [ROADMAP.md](ROADMAP.md); being on the roadmap is not a release commitment.

## Releases

- Semantic-version tags `vX.Y.Z`; a tag push runs the release workflow (GPG signing, CycloneDX SBOM, Maven Central
  publication) and creates the GitHub Release with the SBOM asset.
- Artifacts are immutable on Maven Central; the generated OpenAPI document reports the version so deployments can be
  verified.
- Release notes are the CHANGELOG section for the tag.

## Security

- Vulnerabilities are reported privately per [SECURITY.md](SECURITY.md); fixes ship in a patch release and the advisory
  is published after the fixed release.
- The public CI runs Trivy and dependency review; the release pipeline signs and publishes an SBOM.

## Continuity

Single-maintainer risk is stated honestly. If the maintainer becomes unavailable, the repository remains usable: the
code is Apache-2.0, the release pipeline and docs are self-contained, and consumers can fork. A second maintainer or a
documented succession plan is an explicit 1.0 exit criterion ([ROADMAP.md](ROADMAP.md)).
