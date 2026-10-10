# Contributing to BPMNFlow

Thank you for your interest in contributing to BPMNFlow! This document explains how to get started, how to run the project locally, and what we expect from contributions.

---

## Table of Contents

- [Getting Started](#getting-started)
- [Running the Tests](#running-the-tests)
- [How to Contribute](#how-to-contribute)
- [Commit Convention](#commit-convention)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Versioning](#versioning)
- [License](#license)
- [Code of Conduct](#code-of-conduct)

---

## Getting Started

### Prerequisites

- Java 21+ (CI builds on JDK 21 and JDK 25)
- Maven 3.8+
- Git

### Setup

```bash
git clone https://github.com/jefersonferr/bpmnflow-core.git
cd bpmnflow-core
mvn compile
```

---

## Running the Tests

```bash
mvn test
```

All tests must pass before submitting a pull request. The test suite covers 17 BPMN models with different topologies and validation scenarios.

If you add a new feature, please add at least one test that covers it.

To run the same build as CI — tests plus the JaCoCo coverage gate (80% line / 75% branch) — use:

```bash
mvn clean verify
```

No GPG key, passphrase or publishing credential is needed: artifact signing and Maven Central publication only exist in the opt-in `release` profile (see [Cutting a release](#cutting-a-release)).

---

## How to Contribute

1. **Fork** the repository on GitHub.
2. **Create a branch** from `master` with a descriptive name:
   ```bash
   git checkout -b fix/npe-in-config-loader
   git checkout -b feat/parallel-gateway-support
   ```
3. **Make your changes** — keep them focused. One concern per pull request.
4. **Run the build** (`mvn clean verify`) and make sure everything passes.
5. **Commit** following the convention below.
6. **Push** your branch and open a pull request against `master`.

---

## Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short description>
```

Common types:

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `test` | Adding or updating tests |
| `refactor` | Code change that is not a fix or feature |
| `docs` | Documentation only |
| `chore` | Build, tooling, or dependency updates |

Examples:

```
feat: add support for ParallelGateway
fix: prevent NPE when config property is absent
test: add coverage for empty BPMN model
docs: update README with Spring Boot integration example
chore: upgrade snakeyaml to 2.5
```

---

## Pull Request Guidelines

- Keep pull requests small and focused — one concern per PR makes review faster.
- Include a clear description of what the PR does and why.
- Reference any related issue with `Closes #123` in the PR description.
- All existing tests must pass, on every JDK in the CI matrix.
- New behavior must be covered by new tests.
- Do not include unrelated changes (formatting, refactors) mixed with bug fixes or features.
- If the change breaks the public API or raises the Java baseline, say so explicitly in the PR description.

---

## Versioning

BPMNFlow follows [Semantic Versioning](https://semver.org/):

- **MAJOR** — incompatible changes to the public API or a higher Java baseline.
- **MINOR** — new, backward-compatible features.
- **PATCH** — backward-compatible bug fixes.

The version on `master` always ends in `-SNAPSHOT` (e.g. `4.0.0-SNAPSHOT`) while development is in progress. The suffix is removed only when a release is cut and published to Maven Central.

---

## Cutting a release

Releases are manual and local, performed by the maintainer. Signing and Maven Central publication exist only in the `release` profile, which is never activated automatically — it must be requested with `-Prelease`.

1. **Set the release version.** Remove the `-SNAPSHOT` suffix from the version in `pom.xml` (e.g. `4.1.0`).
2. **Build and sign.** In your own terminal, so the GPG pinentry prompt can ask for the passphrase:
   ```bash
   export GPG_TTY=$(tty)
   mvn -Prelease clean verify
   ```
   The passphrase is typed only in the gpg-agent pinentry prompt; it is never passed on the command line or through an environment variable.
3. **Verify the signatures.** Every `.asc` file in `target/` (library, sources and Javadoc archives) must verify against your public key:
   ```bash
   for f in target/*.asc; do gpg --verify "$f"; done
   ```
4. **Upload to Maven Central.** With the Central credentials configured in your `settings.xml` (server id `central`):
   ```bash
   mvn -Prelease clean deploy
   ```
   By default the bundle is uploaded but **not** published: it is held for manual review (`central.autoPublish` is `false`).
5. **Approve manually.** Open the Central Portal, check that the bundle was validated, and publish it by hand. Publication on Maven Central cannot be undone.
6. **Optional — automatic publication.** Once you trust the process, enable it with a single setting: `mvn -Prelease -Dcentral.autoPublish=true clean deploy`.
7. **Start the next development cycle.** Bump the version to the next `-SNAPSHOT`.

### Troubleshooting

- **`repository element was not specified in the POM inside distributionManagement element`** when deploying a release (non-`-SNAPSHOT`) version: the `-Prelease` flag is missing. The Central repository is only declared in the `release` profile, so without it the build stops before anything is uploaded.
- **Snapshots** keep being deployed to GitHub Packages without `-Prelease`, as before.

---

## License

BPMNFlow is licensed under the [Apache License 2.0](LICENSE). By submitting a pull request, you agree that your contribution is licensed under the same terms, as described in section 5 of the license ("Submission of Contributions").

---

## Code of Conduct

Be respectful and constructive. We welcome contributors of all experience levels. Harassment, discrimination, or hostile behavior of any kind will not be tolerated.

If you have questions, open an issue — we are happy to help.
