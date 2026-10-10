# Data Model: Build Configuration Entities

No persisted data. Entities are build-configuration concepts from the spec.

## Release mode (Maven profile `release`)

| Field | Value |
|-------|-------|
| Activation | Explicit `-Prelease` only; no `<activation>` element (FR-003) |
| Contains | `maven-gpg-plugin` (sign at `verify`), `central-publishing-maven-plugin`, release `<repository>` in `distributionManagement`, property `central.autoPublish` (default `false`) |
| Absent from default build | All of the above (FR-005) |

States: `inactive` (default; no signing, no Central) → `active` (`-Prelease`).

## Publication bundle

Signed jar, sources jar and javadoc jar (plus POM) produced in `target/` when release mode is active. `.asc` files exist only in this state (FR-002, SC-003).

## Publication hold (single setting)

| Property | Default | Meaning |
|----------|---------|---------|
| `central.autoPublish` | `false` | `false`: bundle uploaded and held for manual approval in the Central Portal. `true`: published automatically (FR-009). |

Inspectable via effective POM under `-Prelease` (verification A3). Changing it requires only `-Dcentral.autoPublish=true`.

## Release guard

Rule: a non-SNAPSHOT version deployed without the `release` profile fails before any upload (FR-007). SNAPSHOT versions keep targeting the GitHub Packages `<snapshotRepository>` (FR-008).

| Version | Profile | Result |
|---------|---------|--------|
| SNAPSHOT | none | Deploys to GitHub Packages (credentials required, optional manual check M3) |
| non-SNAPSHOT | none | Fails with Maven's native "repository element was not specified" error, nothing uploaded (documented in `CONTRIBUTING.md` troubleshooting) |
| non-SNAPSHOT | `release` | Signed bundle uploaded to Central, held unless `central.autoPublish=true` |
| SNAPSHOT | `release` | Central rejects the upload (spec edge case); not exercised by any test |