# Implementation Plan: Opt-in Release Signing and Publishing

**Branch**: `001-release-signing-profile` | **Date**: 2026-10-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-release-signing-profile/spec.md`

## Summary

Move GPG signing and Maven Central publishing out of the always-on `<build>` section of `pom.xml` into an opt-in `release` profile that is never auto-activated (`-Prelease` only). The default build (`mvn clean verify`) then needs no key, passphrase or credential, and `-Dgpg.skip` disappears from `ci.yml` and `CONTRIBUTING.md`. Inside the profile, Central publishing defaults to a manual-review hold (`autoPublish=false`) controlled by a single property. The release guard is the Central `<repository>` living only inside the profile, so a release-version deploy without the profile fails before any upload. `CONTRIBUTING.md` documents the everyday command and the release procedure.

Nothing in `src/` changes. No step of this plan, of its verification, or of the implementation may publish to Maven Central.

## Semver Impact

**Public API: none.** No class, method or behavior in `src/` changes; runtime dependencies and the published POM's runtime classpath are unchanged. No `CHANGELOG.md` API entry is required. (FR-012, constitution Principle I.)

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release` 21); CI matrix JDK 21 and 25

**Primary Dependencies**: Build-only: `maven-gpg-plugin` 3.2.8, `central-publishing-maven-plugin` 0.6.0 (both moved into the profile). No new plugin for the release guard (see [research.md](research.md), R3). No runtime dependency change.

**Storage**: N/A

**Testing**: Build-configuration checks (effective POM, active profiles, `target/` contents, temp-copy deploy). The existing JUnit suite and JaCoCo gate (80% line / 75% branch) stay untouched.

**Target Platform**: Maintainer's Linux workstation (release); GitHub Actions `ubuntu-latest` (verification only)

**Project Type**: library (Maven build-configuration change)

**Performance Goals**: N/A

**Constraints**: Single patch `patches/001-lote.patch` including `CONTRIBUTING.md` (Q6, Q7); Release mode only via explicit `-Prelease` (FR-003); snapshot deploy to GitHub Packages unchanged (FR-008); no verification step may publish to Maven Central; passphrase only through the interactive pinentry.

**Scale/Scope**: 3 files: `pom.xml`, `.github/workflows/ci.yml`, `CONTRIBUTING.md`.

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design: still passes.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Stable Public API | Pass | Semver impact declared above: none. |
| II. Minimal Runtime Dependencies | Pass | Only build plugins change; runtime classpath untouched. |
| III. Coverage Gate | Pass | JaCoCo configuration and thresholds not touched; `mvn clean verify` still runs the gate. |
| IV. Credential-Free Everyday Build | Pass (primary driver) | Signing and publishing live only in the opt-in profile. |
| V. Agent Proposes, Maintainer Applies | Pass | Changes go out as `patches/NNN-lote.patch` with a 3–5 line explanation, even for non-`src/` files, so the maintainer applies them. Agent verification runs on a scratch copy with the patch applied (scratchpad), never in the working tree. No `git commit/push/merge/rebase`. Spec Kit artifacts are edited directly. |

No violations; Complexity Tracking not needed.

## Verification Plan (layered, explicit owners)

No layer publishes to Maven Central.

| # | Owner | When | Check |
|---|-------|------|-------|
| A1 | Agent | implement | Compare effective POM without and with `-Prelease`, and `help:active-profiles` in both cases: signing and Central publishing appear only with `-Prelease`. |
| A2 | Agent | implement | `mvn clean verify` without profile passes, no prompt, no `.asc` files under `target/`. |
| A3 | Agent | implement | Effective POM with `-Prelease` shows automatic publication off by default and on when the single property is enabled. Inspection only; no upload. |
| A4 | Agent | implement (US3) | On a temporary copy of the project (outside the working tree), set a release version with `versions:set`, attempt `deploy` without `-Prelease`, confirm it fails before upload. See [quickstart.md](quickstart.md). The Central endpoint must never be reachable by this step (see R4). |
| M1 | Maintainer | own terminal | `mvn -Prelease clean verify` with pinentry prompt, then `gpg --verify` on each `.asc`. |
| M2 | Maintainer | first real release | Confirm in the Central Portal that the bundle is held for manual publication. |
| M3 | Maintainer | optional, manual | Snapshot deploy to GitHub Packages to confirm FR-008 (needs registry credentials). |

## Project Structure

### Documentation (this feature)

```text
specs/001-release-signing-profile/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks, not created here)
```

No `contracts/`: the feature exposes no external interface; the command-line surface (`-Prelease`, one property) is described in `data-model.md` and `quickstart.md`.

### Source Code (repository root)

```text
pom.xml                      # signing + Central publishing + Central <repository> moved into profile `release`; single autoPublish property
.github/workflows/ci.yml     # `mvn verify` without -Dgpg.skip
CONTRIBUTING.md              # plain verify command; new "Cutting a release" section
patches/                     # NNN-lote.patch deliverables (constitution V)
```

**Structure Decision**: Single Maven module; changes confined to the three files above. `src/` untouched.

## Complexity Tracking

No violations to justify.