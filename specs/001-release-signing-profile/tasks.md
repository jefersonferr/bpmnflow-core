# Tasks: Opt-in Release Signing and Publishing

**Input**: Design documents from `/specs/001-release-signing-profile/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md (no `contracts/`: no external interface)

**Tests**: No JUnit tests are added (no `src/` change). Verification is by build-configuration checks (A1–A4, M1–M3) defined in `plan.md` and `quickstart.md`.

**Organization**: Grouped by user story. Every task carries an owner: `[agent]` or `[maintainer]`.

## Format: `- [ ] ID [P?] [Story?] [Owner] Description` (Setup, Foundational, Phase 8 and T020/T023 have no story label)

- **[agent]**: done by the AI agent. All agent edits are made in a local Git clone and end in **one** patch, `patches/001-release-signing-profile.patch` (Q6, Q7).
- **[maintainer]**: done by the maintainer in their own terminal or account.
- **[P]**: can run in parallel (different files, no dependency on incomplete tasks).

## Ground rules (from `CLAUDE.md`, constitution V and the plan)

- **No task may publish to Maven Central.** Only `help:effective-pom`, `help:active-profiles`, `clean verify`, and the US3 temp-copy deploy (which has no Central target) are run by the agent.
- The agent does not edit `pom.xml`, `.github/workflows/ci.yml` or `CONTRIBUTING.md` in the working tree. It edits copies under the scratchpad directory with Edit/Write only (no `sed -i`, `echo >`, `tee`, heredocs), then delivers the diff as a patch.
- The shell-driven file changes `git apply` (T018) and `mvn versions:set` (T009) are allowed **only** on throwaway copies inside the scratchpad directory, never on the real working tree (the real repository only receives `git apply --check`, T017, which is read-only).
- The agent never runs `git commit`, `push`, `merge` or `rebase`.
- Scratch root below is written `$SCRATCH` = the session scratchpad directory.

---

## Phase 1: Setup

**Purpose**: Isolated workspace and baseline for comparison.

- [ ] T001 [agent] Create the working copy as a local Git clone: `git clone <repo path> $SCRATCH/work`, so all edits and builds happen outside the real working tree and `git diff` can later produce root-relative paths.
- [ ] T002 [agent] In `$SCRATCH/work`, capture the baseline before any edit: `mvn -q help:effective-pom -Doutput=$SCRATCH/eff-baseline.xml` and note that `maven-gpg-plugin` and `central-publishing-maven-plugin` are currently in the default build.

---

## Phase 2: Foundational

**Purpose**: Prepare the `pom.xml` change shared by US1–US4.

- [ ] T003 [agent] In `$SCRATCH/work/pom.xml`, remove the `maven-gpg-plugin` and `central-publishing-maven-plugin` blocks (and their explanatory comments) from `<build><plugins>`, keeping their version properties. Keep `maven-source-plugin` and `maven-javadoc-plugin` in the default build (Q3, FR-006).

**Checkpoint**: Default build no longer signs or publishes. The `release` profile is added in US2.

---

## Phase 3: User Story 1 - Credential-free everyday verification (Priority: P1) 🎯 MVP

**Goal**: `mvn clean verify` works on a fresh clone with no key, passphrase or credential, locally and in CI.

**Independent Test**: In `$SCRATCH/work`, `mvn clean verify` passes with no prompt and no `.asc` under `target/`.

- [ ] T004 [US1] [agent] In `$SCRATCH/work/.github/workflows/ci.yml`, change the "Build and test" step command to `mvn verify --batch-mode --no-transfer-progress` (drop `-Dgpg.skip`, Q4, FR-010). Add no secrets and no new workflow (Q5).
- [ ] T005 [US1] [agent] Run A2 in `$SCRATCH/work`: `mvn clean verify`; confirm success with the JaCoCo gate (80% line / 75% branch), no prompt, sources and javadoc jars present, and `find target -name '*.asc'` empty. Then run `mvn clean verify -Dgpg.skip` once and confirm it also passes unchanged (the old flag is harmless).

**Checkpoint**: US1 verified (SC-001, SC-002).

---

## Phase 4: User Story 2 - Explicit, signed release publication (Priority: P1)

**Goal**: Signing and Central publication exist only under `-Prelease`, with the passphrase via gpg-agent pinentry.

**Independent Test**: Effective POM comparison (A1) shows both plugins only with `-Prelease`; maintainer signing check is M1 (Phase 9).

- [ ] T006 [US2] [agent] In `$SCRATCH/work/pom.xml`, add `<profiles><profile><id>release</id>` with **no** `<activation>` block (Q1, FR-003). Inside it add `maven-gpg-plugin` (`${maven-gpg-plugin.version}`, execution `sign-artifacts`, phase `verify`, goal `sign`) with no passphrase configuration, no `--pinentry-mode loopback` and no environment-variable passphrase, and `central-publishing-maven-plugin` (`${central-publishing-maven-plugin.version}`, `<extensions>true</extensions>`, `<publishingServerId>central</publishingServerId>`) (Q2, FR-004, FR-005).
- [ ] T007 [US2] [agent] Run A1 in `$SCRATCH/work`: write effective POMs for default and `-Prelease` (`help:effective-pom -Doutput=...`) and run `help:active-profiles` for both. Confirm the two plugins and profile `release` appear only with `-Prelease`, and diff against `$SCRATCH/eff-baseline.xml` shows nothing else changed in the default build except the removal of those two plugins.

**Checkpoint**: Release mode is opt-in and inspectable.

---

## Phase 5: User Story 3 - Clear failure of an unactivated release deploy (Priority: P2)

**Goal**: A release-version deploy without `-Prelease` fails before any upload; snapshot deploy to GitHub Packages is unchanged. No `maven-enforcer-plugin` (Q8).

**Independent Test**: A4 on a temporary copy; M3 is the optional manual snapshot check.

- [ ] T008 [US3] [agent] In `$SCRATCH/work/pom.xml`, move the Central `<repository>` (`id` `central`, `https://central.sonatype.com/api/v1/publisher`) from the project-level `<distributionManagement>` into the `release` profile's own `<distributionManagement>`. Keep the GitHub Packages `<snapshotRepository>` at project level (Q8, FR-008). Update the XML comments accordingly.
- [ ] T009 [US3] [agent] Run A4 on a **temporary copy** of `$SCRATCH/work` (never the repository working tree and not `$SCRATCH/work` itself): copy to `$SCRATCH/us3-tmp`, run `mvn versions:set -DnewVersion=9.9.9 -DgenerateBackupPoms=false`, then `mvn -s <empty-settings-file> -DskipTests deploy` **without** `-Prelease`. The empty `<settings/>` file is created with Write. Confirm the build fails with Maven's native "repository element was not specified" error before any upload, with no credential or Central target available. Delete `$SCRATCH/us3-tmp` afterwards. Record the observed message (it does not mention `-Prelease`; accepted trade-off Q8).

**Checkpoint**: Guard confirmed by construction and by experiment (SC-004).

---

## Phase 6: User Story 4 - Manual review before first public release (Priority: P2)

**Goal**: Under `-Prelease`, the bundle is held for manual publication by default; one property enables automatic publication. Verified by inspection only.

**Independent Test**: A3: effective POM shows `autoPublish` `false` by default and `true` with `-Dcentral.autoPublish=true`. Nothing is uploaded.

- [ ] T010 [US4] [agent] In `$SCRATCH/work/pom.xml`, add `<properties><central.autoPublish>false</central.autoPublish></properties>` inside the `release` profile (Q9), and set `<autoPublish>${central.autoPublish}</autoPublish>` in the `central-publishing-maven-plugin` configuration (FR-009).
- [ ] T011 [US4] [agent] Run A3 in `$SCRATCH/work`: `mvn -q -Prelease help:effective-pom -Doutput=$SCRATCH/a.xml` and the same with `-Dcentral.autoPublish=true` into `$SCRATCH/b.xml`; `grep -n autoPublish` both. Expect `false` and `true` respectively. Run only `help:effective-pom`; do not run `deploy` or `publish`.

**Checkpoint**: Publication hold default and single setting verified without publishing.

---

## Phase 7: User Story 5 - Documented release process (Priority: P3)

**Goal**: `CONTRIBUTING.md` has the plain verify command and a complete release procedure, in the same patch (Q7).

**Independent Test**: No `gpg.skip` reference remains; every step of Stories 2–4 is documented.

- [ ] T012 [US5] [agent] In `$SCRATCH/work/CONTRIBUTING.md`, replace both `mvn verify -Dgpg.skip` occurrences (the build section and step 4 of "How to Contribute") with `mvn clean verify`, and delete the sentence explaining `-Dgpg.skip` (FR-011, SC-006).
- [ ] T013 [US5] [agent] In `$SCRATCH/work/CONTRIBUTING.md`, add a "Cutting a release" section (English, near "Versioning") covering: remove `-SNAPSHOT` and bump version; `export GPG_TTY=$(tty)` then `mvn -Prelease clean verify` with the interactive pinentry prompt; `gpg --verify` of each `.asc`; deploy with `-Prelease` (default manual-review hold) and approval in the Central Portal; later enabling automatic publication with `-Dcentral.autoPublish=true`; releases are manual and local (Q5). Include a troubleshooting note: if a release deploy fails with "repository element was not specified", `-Prelease` was missing.
- [ ] T014 [US5] [agent] Run `grep -rn "gpg.skip" $SCRATCH/work/CONTRIBUTING.md $SCRATCH/work/.github $SCRATCH/work/pom.xml` and confirm no match.

**Checkpoint**: Documentation consistent with behavior.

---

## Phase 8: Patch delivery

**Purpose**: Single patch for the maintainer (constitution V, Q6).

- [ ] T015 [agent] On the **final** `pom.xml` in `$SCRATCH/work`, repeat all of: (A1) effective POM and `help:active-profiles` with and without `-Prelease` — the gpg and Central plugins and profile `release` appear only with `-Prelease`; (A2) `mvn clean verify` passes, no prompt, no `.asc` in `target/`; (A3) `-Prelease` effective POM shows `autoPublish` `false` by default and `true` with `-Dcentral.autoPublish=true`. Also confirm, in the default effective POM (no deploy), that the GitHub Packages `snapshotRepository` (`id` `github`, `https://maven.pkg.github.com/jefersonferr/bpmnflow-core`) is still present and the Central `<repository>` is absent (FR-008).
- [ ] T016 [agent] Generate the patch with `git -C $SCRATCH/work diff` (the copy is a Git clone, so paths are relative to the repository root: `a/pom.xml`, `b/pom.xml`, `a/.github/workflows/ci.yml`, `a/CONTRIBUTING.md`). Print the diff to the terminal and write exactly that content with the Write tool to `patches/001-release-signing-profile.patch` (create `patches/` by writing the file; no shell redirection). It is the only patch of this feature.
- [ ] T017 [agent] In the real repository, run `git apply --check patches/001-release-signing-profile.patch` (read-only; it does not modify the working tree) and attach the command output (or "no output = applies cleanly") to the report and as a note under this task. If it fails, regenerate the diff (T016), rewrite the patch and repeat this check until it passes.
- [ ] T018 [agent] Apply the patch to a second fresh clone (`$SCRATCH/verify-clone`, via `git apply`) and run `mvn clean verify` there to confirm the patch alone produces a passing, credential-free build.
- [ ] T019 [agent] Confirm `plan.md` still declares the public-API semver impact as **none**, and report to the maintainer a 3–5 line explanation of what the patch changes and why (Portuguese reply per `CLAUDE.md`).

**Checkpoint**: All agent work ends at `patches/001-release-signing-profile.patch`.

---

## Phase 9: Maintainer verification

**Purpose**: Steps only the maintainer can do; after applying the patch.

Run in this order:

- [ ] T020 [maintainer] (a) Review and apply `patches/001-release-signing-profile.patch` to the working tree through IntelliJ (the agent never applies it).
- [ ] T021 [US1] [maintainer] (b) Run `mvn clean verify` in the real working tree; confirm it passes with no prompt and no `.asc` files in `target/`.
- [ ] T022 [US2] [maintainer] (c) M1: in your own terminal run `export GPG_TTY=$(tty)` and `mvn -Prelease clean verify`; confirm the pinentry prompt appears and `.asc` files exist for the jar, sources and javadoc; then run `gpg --verify` on each `.asc` and confirm a good signature (SC-003). This does not upload. Optional: if no signing key is available, confirm the build fails at the signing step, before any upload.
- [ ] T023 [maintainer] (d) Commit the change yourself (the agent never commits).
- [ ] T024 [US1] [maintainer] (e) Open the pull request and confirm CI is green (`mvn verify --batch-mode --no-transfer-progress`, JDK 21 and 25, no signing flag).
- [ ] T025 [US4] [maintainer] (f) M2: at the first real release, follow "Cutting a release" in `CONTRIBUTING.md` with default settings and confirm in the Maven Central Portal that the bundle is validated and held for manual publication before approving (SC-005).
- [ ] T026 [US3] [maintainer] (g) M3 (optional, manual): with GitHub Packages credentials in your own `settings.xml`, deploy a `-SNAPSHOT` version without `-Prelease` and confirm it reaches the GitHub Packages registry as before (FR-008).

---

## Dependencies & Execution Order

- **Phase 1 → Phase 2 → Phases 3–7 → Phase 8 → Phase 9.**
- All `pom.xml` tasks (T003, T006, T008, T010) edit the same file, so they run sequentially in that order. T004 (`ci.yml`) and T012–T013 (`CONTRIBUTING.md`) touch other files.
- US1: T003 → T005 (T004 independent). US2: T006 → T007. US3: T008 (after T006) → T009. US4: T010 (after T006) → T011. US5: T012 → T013 → T014.
- T015–T019 require every earlier agent task; T017 must pass before T018. Phase 9 requires T016–T017 (patch exists and applies); T020–T026 run strictly in the listed order (a)–(g).

### Parallel Opportunities

- T004 [P with T003, T006, T008, T010] (different file: `ci.yml`).
- T012–T013 can run in parallel with the `pom.xml` tasks (different file: `CONTRIBUTING.md`); T014 waits for T004 and T012.
- None among the maintainer tasks: they are sequential (T025 only becomes possible at the first real release; T026 is optional).

```bash
# Example: edit ci.yml and CONTRIBUTING.md while pom.xml tasks proceed in order
Task: T004 ci.yml step command
Task: T012/T013 CONTRIBUTING.md changes
```

## Implementation Strategy

### MVP First (User Story 1)

1. T001–T003, then T004–T005: the default build is credential-free and CI has no signing flag. Stop and validate.
2. Add US2 (T006–T007), then US3 and US4 (T008–T011), then US5 (T012–T014).
3. Phase 8 packs everything into the single patch; Phase 9 is the maintainer's real-world confirmation.

Note: because delivery is a single patch (Q6), the MVP is a validation checkpoint on the scratch copy, not a separate delivery.

## Notes

- Owner tags `[agent]` / `[maintainer]` appear on every task.
- No task publishes to Maven Central; M2 is the only step that touches the Central Portal and it is the maintainer's real release.
- Avoid: editing the working tree directly, running `versions:set` outside a temporary copy, or running `deploy` with `-Prelease`.