# Research: Opt-in Release Signing and Publishing

## Grilling decisions (Q1–Q9)

Official record of the maintainer's decisions. R1–R7 below implement them; where they differ, this section prevails.

- **Q1**: Profile id `release`, activated only explicitly via `-Prelease`; no `<activation>` block.
- **Q2**: Both `maven-gpg-plugin` and `central-publishing-maven-plugin` move into the `release` profile. `distributionManagement` handling per Q8.
- **Q3**: `maven-source-plugin` and `maven-javadoc-plugin` stay in the default build.
- **Q4**: Remove `-Dgpg.skip` from `ci.yml`; the step becomes `mvn verify --batch-mode --no-transfer-progress`.
- **Q5**: No release workflow (GitHub Actions) in this feature; releases stay manual and local, run by the maintainer.
- **Q6**: Full Spec Kit flow (specify, plan, tasks, implement); final delivery is a single patch in `patches/`. Semver impact declared in `plan.md`: none on the public API.
- **Q7**: `CONTRIBUTING.md` updates go in the same patch.
- **Q8**: The Central `<repository>` moves into the `release` profile; the GitHub Packages `snapshotRepository` stays at project level. No `maven-enforcer-plugin`: Maven's native "repository element was not specified" error is the guard. Trade-off accepted: the native message does not mention `-Prelease`.
- **Q9**: Four verification layers: (1) effective POM / active-profiles comparison and (2) plain `mvn clean verify`, run by the agent; (3) `mvn -Prelease clean verify` with pinentry and `gpg --verify`, run by the maintainer; (4) first real release with publication held for manual approval in the Central portal, run by the maintainer. `central.autoPublish` is a property defaulting to `false` in the profile.
- **Passphrase**: supplied via the gpg-agent pinentry; no `--pinentry-mode loopback` and no environment-variable passphrase.

### Post-grilling additions

These came from the plan refinement, not from the Q1–Q9 grilling. They refine Q9's agent layers and do not conflict with any decision above.

- **A3** (agent): the effective POM with `-Prelease` shows automatic publication off by default and on when `central.autoPublish` is enabled (`-Dcentral.autoPublish=true`). Inspection only; nothing is uploaded.
- **A4** (agent, US3): a release-version deploy without `-Prelease` fails before any upload, tested on a temporary copy of the project (release version set via `versions:set`), never in the working tree. See R4.

## R1. Profile activation

- **Decision**: Profile id `release`, no `<activation>` block, so only `-Prelease` activates it.
- **Rationale**: FR-003 forbids activation by property, environment or JDK. A profile with no activation element cannot auto-activate.
- **Alternatives**: Property-based activation (`-Drelease=true`) rejected: spec requires explicit profile request and it is easy to trigger by accident via inherited properties.

## R2. Contents of the `release` profile

- **Decision**: The profile holds `maven-gpg-plugin` (`sign` goal at `verify`, gpg-agent pinentry; no `--pinentry-mode loopback`, no passphrase configuration or environment variable) and `central-publishing-maven-plugin` (`extensions` true) with `<autoPublish>${central.autoPublish}</autoPublish>`. `central.autoPublish` is declared in the profile's own `<properties>` with value `false` (Q9); the maintainer enables it with `-Dcentral.autoPublish=true`.
- **Rationale**: FR-004, FR-005, FR-009. A property gives the "single setting" and an inspectable effective POM value (A3). With `extensions` true the Central plugin takes over the deploy lifecycle, so keeping it out of the default build also stops it from interfering with snapshot deploys (FR-008).
- **Alternatives**: Hard-coded `autoPublish` in the profile rejected (needs POM edit to change). Putting `autoPublish` only on the command line rejected (default would then depend on plugin default, not visible in the POM).
- **Note**: Source and Javadoc plugins stay in the default build (FR-006). Signing covers the jar, sources and javadoc because the gpg plugin signs all attached artifacts.

## R3. Guard for release deploy without the profile (US3, FR-007)

- **Decision**: Keep `<snapshotRepository>` (GitHub Packages) in the default `distributionManagement`. Move the release `<repository>` (Central) into the `release` profile. The guard is only this: without the profile, a non-snapshot deploy has no target repository and aborts before any upload. No additional plugin is used (grilling decision Q8).
- **Rationale**: Zero upload is guaranteed by construction (SC-004); no extra build machinery.
- **A4 role**: The temp-copy experiment only confirms that a release-version deploy without `-Prelease` fails before any upload.
- **Accepted trade-off (Q8)**: Maven's native error ("repository element was not specified") does not mention `-Prelease`. Clarity comes from documentation: `CONTRIBUTING.md` carries a troubleshooting note (see R6).
- **Alternatives**: `maven-enforcer-plugin` message rejected by Q8.

## R4. Safe procedure for testing US3

- **Decision**: Copy the project into the scratchpad, run `versions:set -DnewVersion=<x.y.z>` there, attempt `deploy` without `-Prelease`, discard the copy. Never change the version in the working tree. Because the copy has no `release` profile active, the Central endpoint is not configured; additionally run with `-DaltDeploymentRepository` NOT set, and with no `central` credentials in `settings.xml` use (`-s` pointing at an empty settings file) so no credential can reach Central.
- **Rationale**: User requirement: never touch the working tree; no step may publish to Central.
- **Alternatives**: Dry-run flags rejected: deploy plugin has no reliable dry-run, and the point is to prove the guard.

## R5. CI

- **Decision**: `ci.yml` runs `mvn verify --batch-mode --no-transfer-progress` (drop `-Dgpg.skip`). No secrets added. The old flag stays harmless if passed (edge case in spec).

## R6. CONTRIBUTING.md

- **Decision**: Replace both `mvn verify -Dgpg.skip` occurrences with `mvn clean verify`, delete the `-Dgpg.skip` explanation, and add a "Cutting a release" section: version bump, `-Prelease clean verify` with `GPG_TTY` and pinentry, `gpg --verify` of the `.asc` files, deploy with the default manual-review hold, confirming in the Central Portal, and the optional `-Dcentral.autoPublish=true` for later releases. The section also has a troubleshooting note: if a release deploy fails with "repository element was not specified", `-Prelease` was missing.

## R7. Delivery under project rules

- **Decision**: Because constitution V and `CLAUDE.md` require patches and forbid shell-based edits, the implement phase produces a single patch, `patches/001-release-signing-profile.patch` (Q6), covering `pom.xml`, `ci.yml` and `CONTRIBUTING.md` (Q7). The edits are made in a local Git clone in the scratchpad and the patch is produced with `git diff` there (root-relative paths); agent checks A1–A4 run in that clone, and `git apply --check` is run against the real repository.