# Feature Specification: Opt-in Release Signing and Publishing

**Feature Branch**: `001-release-signing-profile`

**Created**: 2026-10-09

**Status**: Draft

**Input**: User description: "Incorporate decisions Q1–Q9 from the grilling. Local and CI verification (`mvn verify`) must run without a GPG key or passphrase; release publication must require explicit activation and produce signed artifacts; a release deploy without that activation must fail clearly; the first publication must allow manual review before becoming public; the process must be documented in CONTRIBUTING.md. Write in English."

## Clarifications

### Session 2026-10-09

- Q: How is the publication hold (User Story 4, Scenario 1 / SC-005) verified without uploading to Central? → A: Effective POM check of the default (automatic publication inactive) plus the maintainer's confirmation in the Central portal during the first real release, outside spec tests.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Credential-free everyday verification (Priority: P1)

A contributor, the maintainer's agent, or the CI service clones the repository on a machine with no GPG key, no passphrase and no publishing credentials, and runs the standard verification command. The build compiles, tests, checks coverage thresholds and produces the library, sources and documentation archives without ever asking for a secret.

**Why this priority**: This is constitution Principle IV. Today every verification attempts to sign artifacts, forcing a workaround flag everywhere. Removing that friction is the core value.

**Independent Test**: On a machine with no GPG key configured, run the standard verification command from a fresh clone and confirm it passes with no prompt and no signature files produced.

**Acceptance Scenarios**:

1. **Given** a fresh clone and no signing key or passphrase available, **When** the standard verification command runs without any extra flag, **Then** the build succeeds and no passphrase prompt appears.
2. **Given** the same setup, **When** the build finishes, **Then** no signature files exist among the build outputs.
3. **Given** the CI workflow, **When** it runs on a pull request, **Then** it verifies the project without any signing-related flag.

---

### User Story 2 - Explicit, signed release publication (Priority: P1)

The maintainer cuts a release by explicitly activating the release mode. Only then are artifacts signed (passphrase requested interactively by the system's key prompt) and the bundle prepared for publication to Maven Central. Signed outputs cover the library, sources and documentation archives.

**Why this priority**: Maven Central requires signed artifacts; the release path must remain fully functional while being fenced off from everyday work.

**Independent Test**: Activate the release mode locally on the maintainer's machine, supply the passphrase at the prompt, and confirm every published archive has a valid signature.

**Acceptance Scenarios**:

1. **Given** the maintainer's key is available, **When** the build runs with release mode explicitly activated, **Then** the interactive key prompt appears and a signature is produced for the library, sources and documentation archives.
2. **Given** the produced signatures, **When** each is verified against the maintainer's public key, **Then** all verify successfully.
3. **Given** release mode is not activated, **When** any build runs, **Then** neither signing nor Central publication is configured.

---

### User Story 3 - Clear failure of an unactivated release deploy (Priority: P2)

If someone attempts to deploy a release (non-snapshot) version without activating release mode, the build fails before uploading anything instead of attempting an unsigned or misdirected upload; the troubleshooting documentation explains the cause. Snapshot deployment to the internal package registry keeps working as before.

**Why this priority**: Prevents accidental unsigned or misrouted releases; lower than P1 because it is a guard rail, not the main flow.

**Independent Test**: Attempt a deploy of a release version without activating release mode and confirm it fails before uploading anything.

**Acceptance Scenarios**:

1. **Given** a release (non-snapshot) version and no release mode, **When** a deploy is attempted, **Then** the build fails and nothing is uploaded.
2. **Given** a snapshot version and no release mode, **When** a deploy is attempted with valid registry credentials, **Then** it targets the internal GitHub Packages registry as it does today.

---

### User Story 4 - Manual review before first public release (Priority: P2)

For the first publication using the new setup, the bundle is uploaded to Maven Central but is not made public automatically; the maintainer reviews the validation result in the Central portal and publishes manually. Automatic publication can be enabled later through a single setting without restructuring the build.

**Why this priority**: Publication to Maven Central is irreversible; a human gate lowers the risk of the first real run.

**Independent Test**: With automatic publication enabled by the single setting, inspect the build's effective configuration (effective POM) and confirm it shows automatic publication as active. Nothing is uploaded or published to Maven Central.

**Acceptance Scenarios**:

1. **Given** release mode with default settings, **When** the effective POM is inspected (no upload performed), **Then** it shows automatic publication as inactive, so the bundle would be held for manual publication. The actual hold in the Central portal is confirmed only by the maintainer during the first real release, outside this spec's tests.
2. **Given** release mode with automatic publication explicitly enabled by a single setting, **When** the effective POM is inspected (no upload performed), **Then** it shows automatic publication as active.

---

### User Story 5 - Documented release process (Priority: P3)

A contributor reading `CONTRIBUTING.md` finds the correct everyday verification command (no skip flag), and a section describing how to cut a release: activating release mode, the interactive passphrase prompt, the manual-review first publication, and the validation steps.

**Why this priority**: Documentation supports the other stories but does not change behavior.

**Independent Test**: Read `CONTRIBUTING.md` and confirm no reference to the old skip flag remains and the release procedure is complete and consistent with actual behavior.

**Acceptance Scenarios**:

1. **Given** the updated `CONTRIBUTING.md`, **When** a contributor looks for how to verify a change, **Then** they find the plain verification command with no signing-skip flag.
2. **Given** the same document, **When** the maintainer looks for the release procedure, **Then** each step from Stories 2–4 is described.

---

### Edge Cases

- Release mode activated but no signing key is present: the build fails at signing, before any upload.
- Release mode activated on a snapshot version: the Central upload is rejected rather than published.
- A contributor still passes the old skip flag: it is harmless and the build still passes.
- Release mode activated in CI: out of scope (no release workflow exists); only manual local releases are supported.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The standard verification command MUST succeed without any GPG key, passphrase or publishing credential and without any extra flag.
- **FR-002**: Outside release mode, builds MUST NOT produce signature files and MUST NOT prompt for a passphrase.
- **FR-003**: Release mode MUST be activated only by explicit request; it MUST NOT activate automatically by property, environment or JDK.
- **FR-004**: In release mode, the library, sources and documentation archives MUST be signed, with the passphrase supplied through the interactive key prompt.
- **FR-005**: Signing and Central publication configuration MUST exist only in release mode.
- **FR-006**: Source and documentation archives MUST continue to be produced in the standard build.
- **FR-007**: A deploy of a release (non-snapshot) version without release mode MUST fail before uploading anything. Clarity about the cause comes from the documentation, not from the build message.
- **FR-008**: Snapshot deployment to the GitHub Packages registry MUST remain unchanged.
- **FR-009**: Release mode MUST default to holding the bundle for manual publication, with a single setting to enable automatic publication.
- **FR-010**: The CI workflow MUST verify the project without any signing-skip flag.
- **FR-011**: `CONTRIBUTING.md` MUST replace the signing-skip verification command with the plain command and document the release procedure, including activation, passphrase prompt, manual-review first publication and validation steps.
- **FR-012**: The change MUST NOT alter the public API, runtime dependencies or coverage thresholds (constitution Principles I–III); the plan MUST declare no public-API semver impact.

### Key Entities

- **Release mode**: An explicitly activated build configuration that adds artifact signing and Central publication.
- **Publication bundle**: The set of signed library, sources and documentation archives uploaded to Maven Central.
- **Publication hold**: The state in which an uploaded bundle awaits manual approval in the Central portal.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of standard verification runs (local and CI) complete with zero secret prompts and zero signing flags.
- **SC-002**: A fresh clone on a machine with no keys passes verification on the first attempt.
- **SC-003**: 100% of archives produced in release mode have a signature that verifies against the maintainer's public key.
- **SC-004**: An unactivated release deploy attempt fails with zero bytes uploaded.
- **SC-005**: With default settings, the effective POM in release mode shows automatic publication inactive; the first real release publication then requires exactly one manual approval step in the Central portal before becoming public (confirmed by the maintainer, not by a spec test).
- **SC-006**: `CONTRIBUTING.md` contains no reference to the old signing-skip flag and documents every release step.

## Assumptions

- Releases remain manual and local, performed by the maintainer; no release workflow is created in this feature.
- The maintainer's key and Central portal account are already set up (version 4.0.0 was already published).
- The passphrase is always provided through the system's interactive key prompt; no non-interactive passphrase mechanism is needed.
- GitHub Packages snapshot deployment credentials are managed in the maintainer's own settings and are not part of everyday verification.
- Verification of the standard build and configuration comparison is performed by the agent during implementation; signing and the first real publication are performed by the maintainer.
- No test or verification defined in this spec may publish to Maven Central; publication-related behavior is verified only by inspecting configuration (e.g., the effective POM).