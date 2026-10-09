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
mvn verify -Dgpg.skip
```

`-Dgpg.skip` disables artifact signing, which is only required when publishing to Maven Central.

---

## How to Contribute

1. **Fork** the repository on GitHub.
2. **Create a branch** from `master` with a descriptive name:
   ```bash
   git checkout -b fix/npe-in-config-loader
   git checkout -b feat/parallel-gateway-support
   ```
3. **Make your changes** — keep them focused. One concern per pull request.
4. **Run the build** (`mvn verify -Dgpg.skip`) and make sure everything passes.
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

## Code of Conduct

Be respectful and constructive. We welcome contributors of all experience levels. Harassment, discrimination, or hostile behavior of any kind will not be tolerated.

If you have questions, open an issue — we are happy to help.