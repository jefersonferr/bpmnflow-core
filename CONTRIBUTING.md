# Contributing to BPMNFlow

Thank you for your interest in contributing to BPMNFlow! This document explains how to get started, how to run the project locally, and what we expect from contributions.

---

## Table of Contents

- [Getting Started](#getting-started)
- [Running the Tests](#running-the-tests)
- [How to Contribute](#how-to-contribute)
- [Commit Convention](#commit-convention)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Code of Conduct](#code-of-conduct)

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Git

### Setup

```bash
git clone https://github.com/jefersonferr/bpmn-model-parser.git
cd bpmn-model-parser
mvn compile
```

---

## Running the Tests

```bash
mvn test
```

All tests must pass before submitting a pull request. The test suite covers 9 BPMN models with different topologies and validation scenarios.

If you add a new feature, please add at least one test that covers it.

---

## How to Contribute

1. **Fork** the repository on GitHub.
2. **Create a branch** from `main` with a descriptive name:
   ```bash
   git checkout -b fix/npe-in-config-loader
   git checkout -b feat/parallel-gateway-support
   ```
3. **Make your changes** — keep them focused. One concern per pull request.
4. **Run the tests** and make sure everything passes.
5. **Commit** following the convention below.
6. **Push** your branch and open a pull request against `main`.

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
chore: upgrade snakeyaml to 2.1
```

---

## Pull Request Guidelines

- Keep pull requests small and focused — one concern per PR makes review faster.
- Include a clear description of what the PR does and why.
- Reference any related issue with `Closes #123` in the PR description.
- All existing tests must pass.
- New behavior must be covered by new tests.
- Do not include unrelated changes (formatting, refactors) mixed with bug fixes or features.

---

## Code of Conduct

Be respectful and constructive. We welcome contributors of all experience levels. Harassment, discrimination, or hostile behavior of any kind will not be tolerated.

If you have questions, open an issue — we are happy to help.
