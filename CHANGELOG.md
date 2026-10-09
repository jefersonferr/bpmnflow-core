# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## 4.0.0 - Unreleased

### Breaking

- **Java 21 is now the minimum version** (was 17). CI builds and tests on JDK 21 and JDK 25.
- **License changed from MIT to Apache License 2.0.** The project remains free for any use, now with an explicit patent grant. Versions up to and including 3.2.0 remain available under MIT.
- Unreadable models (missing stream, malformed XML, non-BPMN content) throw the new unchecked `BpmnModelException` instead of a plain `RuntimeException`. Existing `catch (RuntimeException e)` blocks still work.
- Camunda 7 connectors: a `headers` input parameter written as `<camunda:map>` is now mapped to `taskHeaders`, one entry per map entry. Other custom input parameters go only to `inputMappings` and are no longer duplicated into `taskHeaders`.
- Activities, and the rules derived from them, now follow model order instead of hash order.

### Added

- **Operaton and CIB seven support.** New `engine` values `operaton` and `cibseven` (aliases of the Camunda 7 adapter). The Camunda 7 adapter reads both the `camunda:` and the `operaton:` namespaces, even mixed in one model.
- `RuleType.isInitial()` and `RuleType.isFinal()`; `WorkflowRule.isInitial()`, `WorkflowRule.isFinal()` and `WorkflowRule.endpointsValid(...)`.
- `UnsupportedElementHandler`: parallel, inclusive, event-based and complex gateways, boundary and intermediate events, sub-processes and call activities are reported with a `WARNING` log entry instead of being ignored silently.
- `Automatic-Module-Name: org.bpmnflow.core` in the jar manifest.
- `CHANGELOG.md`, `NOTICE`, and the README sections *Compatibility*, *Licensing — zero cost*, *Parsing Behavior* and *Upgrading from 3.x*.
- Test models for multiple participants, merge+split gateways, Operaton and mixed namespaces (17 models in total).

### Fixed

- Rules with an accidental `null` endpoint are no longer produced. Only initial rules have a `null` source and only final rules have a `null` target; `WorkflowRule` enforces this in its constructor and setters.
- Task `documentation` is read from the `<bpmn:documentation>` child element (it was always `null`).
- Models with several pools: header data and stages come from the first pool that has a process and are no longer overwritten by later pools. Black-box pools are ignored; extra pools with a process are logged as a `WARNING`.
- Rule 6 (split → merge): every activity that reaches a merge+split gateway now gets its `SPLIT_TO_MERGE` rule, not only the first one.
- Missing-property warnings from `BpmnPropertiesLoader` are logged once per element type instead of once per element.

### Changed

- `zeebe-bpmn-model` 8.6.5 → 8.8.41, SnakeYAML 2.3 → 2.5, Lombok 1.18.30 → 1.18.48, JUnit 5.11.3 → 5.14.4, JaCoCo 0.8.12 → 0.8.15, and Maven plugin updates. Plugin and dependency versions are centralized in `pom.xml` properties.
- README rewritten for 4.0.0: corrected API handler examples (`camunda:connector` / `zeebe:taskDefinition`) and code samples.

## 3.2.0 - 2026-06-03

### Added

- API handler service tasks: `ApiActivityNode`, `ApiHandlerDefinition` and `ApiField`.

Earlier versions are documented in the [Git history](https://github.com/jefersonferr/bpmnflow-core/commits/master).
