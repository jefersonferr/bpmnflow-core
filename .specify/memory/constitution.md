<!--
Sync Impact Report (temporary; remove before committing)
- Version change: (template, unversioned) → 1.0.0
- Modified principles: none renamed (initial adoption; all five template slots filled)
- Added sections: Core Principles I–V, Technical Constraints, Development Workflow, Governance
- Removed sections: none
- Deferred items: none
- Templates reviewed (not modified): plan/spec/tasks templates read the constitution at runtime
-->
# bpmnflow-core Constitution

## Core Principles

### I. Stable Public API

bpmnflow-core is a Java 21 library licensed under Apache 2.0 and published to Maven Central.
Its public API is a contract with downstream consumers.

- Every change that touches the public API MUST declare its semver impact (patch, minor or
  major) in the feature's `plan.md`.
- Breaking changes MUST only ship in a major release and MUST be recorded in `CHANGELOG.md`.
- Removals MUST be preceded by deprecation in at least one minor release, unless the plan
  justifies an exception.
- Anything not intended as public MUST NOT be exposed (package-private or internal packages).

Rationale: consumers pin versions by semver; an undeclared break erodes trust in the artifact.

### II. Minimal Runtime Dependencies

The only runtime dependencies permitted are `zeebe-bpmn-model` and `snakeyaml`.

- Adding any other runtime-scoped dependency is prohibited without a constitutional amendment.
- Compile-time-only tools (e.g. Lombok, `provided` scope) and test-scope dependencies are not
  runtime dependencies, but MUST NOT leak into the public API or the published POM's runtime
  classpath.
- Transitive runtime dependencies MUST be reviewed whenever the two allowed ones are upgraded.

Rationale: a lean classpath avoids version conflicts in consumers' applications.

### III. Test Coverage Gate (NON-NEGOTIABLE)

JaCoCo MUST enforce a minimum of 80% line coverage and 75% branch coverage in the Maven build.

- The `verify` build MUST fail when either threshold is not met.
- Thresholds MUST NOT be lowered or bypassed (excludes, skips) to make a change pass.
- New behavior MUST ship with tests in the same change.

Rationale: coverage thresholds are the automated guard for the stability promised in Principle I.

### IV. Credential-Free Everyday Build

The daily workflow (`mvn clean verify`, tests, coverage, local install) MUST NOT require
credentials, tokens or keys.

- GPG signing and Central publishing MUST live in an opt-in release profile only.
- Secrets MUST NOT be committed to the repository, nor required by CI jobs that only build
  and test.
- A fresh clone on a machine with no secrets MUST build and pass tests.

Rationale: contributors and agents must be able to build and verify without privileged access.

### V. Agent Proposes, Maintainer Applies

AI agents working in this repository propose changes; the maintainer applies and commits them.

- Agents MUST NOT run `git commit`, `push`, `merge` or `rebase`.
- Changes to `src/` MUST be delivered as unified patches in `patches/NNN-lote.patch`,
  accompanied by a short explanation of what changes and why.
- Spec Kit artifacts (`specs/`, `.specify/`) MAY be edited directly.
- Files MUST be modified through the Edit/Write tools, not through shell redirection or
  in-place editing commands.

Rationale: the maintainer retains review authority and ownership of repository history.

## Technical Constraints

- Language and runtime: Java 21 (`maven.compiler.release` 21).
- Build tool: Maven; artifacts published to Maven Central with sources and Javadoc attached.
- License: Apache 2.0; `LICENSE` and `NOTICE` MUST be kept accurate.
- Maintainer-facing documentation, project instructions and agent replies are in Portuguese
  unless a document states otherwise; this constitution is in English by decision.

## Development Workflow

- Each feature follows Spec Kit: specify → plan → tasks → implement.
- `plan.md` MUST include a "Constitution Check" covering Principles I–V, and the semver impact
  of any public API change.
- Work is delivered in batches; each batch yields one patch with a 3–5 line explanation.
- Before a patch is proposed, the build (`mvn clean verify`) MUST pass with coverage gates.
- Releases (version bump, signing, publishing) are performed by the maintainer only.

## Governance

This constitution supersedes other practices in the repository. Amendments require a pull
request or commit by the maintainer that updates this file, states the rationale, and includes a
migration note when existing work is affected. Agents MAY draft amendments but MUST NOT commit
them.

Versioning of this document follows semver: MAJOR for removing or redefining a principle in an
incompatible way, MINOR for adding a principle or materially expanding guidance, PATCH for
clarifications and wording fixes.

Compliance MUST be checked in every `plan.md` Constitution Check and during review of every
patch; violations MUST be justified in the plan or the change is rejected. Runtime guidance for
agents lives in `CLAUDE.md`.

**Version**: 1.0.0 | **Ratified**: 2026-10-09 | **Last Amended**: 2026-10-09