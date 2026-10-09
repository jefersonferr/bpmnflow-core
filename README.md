# BPMNFlow

> Lightweight BPMN model parser for model-driven workflow automation

![Java](https://img.shields.io/badge/Java-21+-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-orange)
![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)
![BPMN Support](https://img.shields.io/badge/BPMN-2.0-brightgreen)
![Camunda 7](https://img.shields.io/badge/Camunda-7-blue)
![Camunda 8](https://img.shields.io/badge/Camunda-8-purple)
![Operaton](https://img.shields.io/badge/Operaton-supported-teal)
![CIB seven](https://img.shields.io/badge/CIB_seven-supported-teal)
![CI](https://github.com/jefersonferr/bpmnflow-core/actions/workflows/ci.yml/badge.svg)

---

## Table of Contents

- [Why BPMNFlow?](#why-bpmnflow)
- [BPMNFlow vs Traditional BPMN Engines](#bpmnflow-vs-traditional-bpmn-engines)
- [Quick Start](#quick-start)
- [Compatibility](#compatibility)
- [Licensing — zero cost](#licensing--zero-cost)
- [Engine Support](#engine-support)
- [Extension Properties](#extension-properties)
- [API Handler Service Tasks](#api-handler-service-tasks)
- [YAML Configuration](#yaml-configuration)
- [Parsing Behavior](#parsing-behavior)
- [Upgrading from 3.x](#upgrading-from-3x)
- [Use Cases](#use-cases)
- [What's Next](#whats-next)
- [License](#license)

---

## Why BPMNFlow?

Most projects that adopt BPMN don't need a full workflow engine — they need to **read business intent from a diagram and act on it**. BPMNFlow solves exactly that, without the infrastructure overhead of a process engine.

- **Lightweight** — no runtime engine, no state persistence, no database
- **Model-as-Code** — BPMN becomes a dynamic configuration layer
- **YAML-driven validation** — define which properties are required per element type
- **Zero lock-in** — works with any architecture or framework
- **Multi-engine** — parses models from Camunda 7, Camunda 8, Operaton and CIB seven
- **Zero cost** — Apache 2.0, no license fee whatever modeler or engine produced the model
- **API handler support** — service tasks carry API call definitions parsed directly from the model
- **Spring Boot ready** — see [bpmnflow-spring-boot-starter](https://github.com/jefersonferr/bpmnflow-spring-boot-starter)

---

## BPMNFlow vs Traditional BPMN Engines

| Feature                      | BPMNFlow      | Camunda / Operaton / Flowable |
|------------------------------|---------------|-------------------------------|
| Runtime engine               | No            | Yes                           |
| State management             | No            | Yes                           |
| Lightweight                  | Yes           | No                            |
| Cloud-native                 | High          | Medium                        |
| Model parsing                | Yes           | Yes                           |
| Setup complexity             | Low           | High                          |
| Camunda 7 / CIB seven models | Yes           | Engine-specific               |
| Operaton models              | Yes           | Engine-specific               |
| Camunda 8 models             | Yes           | Engine-specific               |

Use BPMNFlow when you need **interpretation**, not orchestration.

---

## Quick Start

### Installation
```xml
<dependency>
    <groupId>org.bpmnflow</groupId>
    <artifactId>bpmnflow-core</artifactId>
    <version>4.0.0</version>
</dependency>
```

Requires **Java 21 or newer**.

### Basic Usage
```java
import org.bpmnflow.model.Workflow;
import org.bpmnflow.parser.BpmnPropertiesConfig;
import org.bpmnflow.parser.ConfigLoader;
import org.bpmnflow.parser.ModelParser;

try (InputStream model      = Files.newInputStream(Path.of("process.bpmn"));
     InputStream configYaml = Files.newInputStream(Path.of("bpmn-config.yaml"))) {

    BpmnPropertiesConfig config = ConfigLoader.loadConfig(configYaml);
    Workflow workflow = ModelParser.parser(model, config);

    System.out.println("Name:        " + workflow.getName());
    System.out.println("Valid:       " + workflow.getInconsistencies().isEmpty());
    System.out.println("Activities:  " + workflow.activitiesSize());
    System.out.println("Rules:       " + workflow.rulesSize());
}
```

`ModelParser.parser(InputStream, String)` is also available when the YAML lives on the file system.

### Listing API handler activities
```java
workflow.getActivities().forEach(activity -> {
    if (activity instanceof ApiActivityNode api) {
        ApiHandlerDefinition handler = api.getApiHandler();
        System.out.println(api.getAbbreviation() + " -> " + handler.getEndpoint());
    }
});
```

---

## Compatibility

### Java

| Item              | Supported                                    |
|-------------------|----------------------------------------------|
| Java baseline     | 21 (`maven.compiler.release=21`)             |
| Tested in CI      | JDK 21 and JDK 25                            |
| JPMS module name  | `org.bpmnflow.core` (`Automatic-Module-Name`) |

### Model formats

| Models created for          | `engine` value            | Extension namespace                                         |
|-----------------------------|---------------------------|-------------------------------------------------------------|
| Camunda 7                   | `camunda7` (default)      | `camunda:` — `http://camunda.org/schema/1.0/bpmn`           |
| CIB seven                   | `cibseven`                | `camunda:` — `http://camunda.org/schema/1.0/bpmn`           |
| Operaton                    | `operaton`                | `operaton:` — `http://operaton.org/schema/1.0/bpmn` (and `camunda:`) |
| Camunda 8 (Zeebe)           | `camunda8`                | `zeebe:` — `http://camunda.org/schema/zeebe/1.0`            |

Operaton and CIB seven are community forks of Camunda 7, so they share the Camunda 7 adapter:

- **CIB seven** keeps the `camunda:` namespace — its models are read exactly like Camunda 7 models.
- **Operaton** uses the `operaton:` namespace and still accepts `camunda:`. BPMNFlow reads **both**, even mixed in the same model.
- `operaton` and `cibseven` are aliases of `camunda7`; they only change the engine id reported by the adapter. Using `camunda7` for an Operaton model also works.

Models can be authored in **Camunda Modeler** (desktop or web), the **Operaton Modeler**, or any **bpmn.io**-based tool — BPMNFlow reads the `.bpmn` XML, not the tool.

---

## Licensing — zero cost

**Using BPMNFlow never requires a license purchase, subscription or fee — whatever engine or modeler the BPMN model was made for.**

- BPMNFlow is released under the **[Apache License 2.0](LICENSE)**: free for personal and commercial use, modification and redistribution, with an explicit patent grant.
- BPMNFlow **does not embed, call or require any process engine**. It reads `.bpmn` XML files. Parsing a Camunda 8 model is not running Camunda 8.
- The model API it depends on, `io.camunda:zeebe-bpmn-model`, is published by Camunda under **Apache 2.0** — the same license as BPMNFlow. SnakeYAML is also Apache 2.0. Lombok is compile-time only and is not shipped.

| Scenario                                                    | Cost with BPMNFlow |
|-------------------------------------------------------------|--------------------|
| Parse Camunda 7 / CIB seven / Operaton models               | Zero               |
| Parse Camunda 8 models, in development or in production     | Zero               |
| Use BPMNFlow in a commercial, closed-source product         | Zero               |

> **What BPMNFlow does not change:** if you *also run* a process engine, that engine's own license applies to it. For example, running Camunda 8 (8.6+) self-managed in production is governed by the Camunda License — that is a matter between you and the engine vendor, independent of BPMNFlow. Operaton and CIB seven are open source (Apache 2.0).
>
> This section describes licenses, not legal advice. Check `mvn dependency:tree` and your own policies for your specific build.

---

## Engine Support

The target engine is declared in `bpmn-config.yaml` via the `engine` field. Accepted values: `camunda7` (default), `operaton`, `cibseven`, `camunda8`.

### Camunda 7 and CIB seven

Extension properties use the `camunda:` namespace (`http://camunda.org/schema/1.0/bpmn`). The version tag is a native attribute on the `<process>` element.
```yaml
bpmn_model_parser:
  engine: camunda7   # default — can be omitted; use 'cibseven' for CIB seven
  model_properties:
    ...
```
```xml
<bpmn:task id="Task_1" name="My Task">
  <bpmn:extensionElements>
    <camunda:properties>
      <camunda:property name="stage"    value="ST" />
      <camunda:property name="activity" value="AC1" />
    </camunda:properties>
  </bpmn:extensionElements>
</bpmn:task>

<bpmn:process id="Process_1" camunda:versionTag="1.0">
```

### Operaton

Same structure as Camunda 7 with the `operaton:` namespace (`http://operaton.org/schema/1.0/bpmn`). Elements using `camunda:` in the same model are read too.
```yaml
bpmn_model_parser:
  engine: operaton
  model_properties:
    ...
```
```xml
<bpmn:task id="Task_1" name="My Task">
  <bpmn:extensionElements>
    <operaton:properties>
      <operaton:property name="stage"    value="ST" />
      <operaton:property name="activity" value="AC1" />
    </operaton:properties>
  </bpmn:extensionElements>
</bpmn:task>

<bpmn:process id="Process_1" operaton:versionTag="1.0">
```

### Camunda 8

Extension properties use the `zeebe:` namespace (`http://camunda.org/schema/zeebe/1.0`). The version tag is a child element inside `extensionElements`.
```yaml
bpmn_model_parser:
  engine: camunda8
  model_properties:
    ...
```
```xml
<bpmn:task id="Task_1" name="My Task">
  <bpmn:extensionElements>
    <zeebe:properties>
      <zeebe:property name="stage"    value="ST" />
      <zeebe:property name="activity" value="AC1" />
    </zeebe:properties>
  </bpmn:extensionElements>
</bpmn:task>

<bpmn:process id="Process_1">
  <bpmn:extensionElements>
    <zeebe:versionTag value="1.0" />
  </bpmn:extensionElements>
</bpmn:process>
```

### Engine compatibility matrix

| Feature               | `camunda7` / `cibseven`         | `operaton`                             | `camunda8`                         |
|-----------------------|---------------------------------|----------------------------------------|------------------------------------|
| Extension properties  | `<camunda:property>`            | `<operaton:property>` or `<camunda:property>` | `<zeebe:property>`          |
| Version tag           | `camunda:versionTag` attribute  | `operaton:versionTag` or `camunda:versionTag` | `<zeebe:versionTag>` element |
| API handler           | `<camunda:connector>`           | `<operaton:connector>` or `<camunda:connector>` | `<zeebe:taskDefinition>`   |
| Default               | Yes (`camunda7` when absent)    | No                                     | No                                 |

---

## Extension Properties

Extension properties let you embed custom metadata directly in BPMN elements. BPMNFlow reads these properties and maps them to the parsed `Workflow` object.

> **The properties listed below are not fixed** — they are simply the ones extracted and validated based on what you define in your `bpmn-config.yaml`. You have full control over which properties are read, which are required, and which elements they apply to. See [YAML Configuration](#yaml-configuration) for details.

### Supported elements and properties

| Element         | Property          | Description                                              |
|-----------------|-------------------|----------------------------------------------------------|
| `Participant`   | `process_type`    | Process classification — any value defined in your model |
| `Participant`   | `process_subtype` | Process subtype — any value defined in your model        |
| `Lane`          | `stage`           | Stage code for the lane                                  |
| `Task`          | `stage`           | Stage the task belongs to                                |
| `Task`          | `activity`        | Activity code within the stage                           |
| `StartEvent`    | `process_status`  | Initial process status                                   |
| `EndEvent`      | `process_status`  | Final process status                                     |
| `SequenceFlow`  | `conclusion`      | Conclusion code that triggers this path                  |
| `SequenceFlow`  | `process_status`  | Resulting status after the transition                    |

### Defining properties in the modeler

Open any element → **Properties Panel** → **Extension Properties** → click **+**.

Works the same way in Camunda Modeler (C7 and C8 diagrams) and in the Operaton Modeler — the correct namespace is applied automatically by the tool.

---

## API Handler Service Tasks

Since `3.2.0`, BPMNFlow parses service tasks that carry an API call definition. When a service task has a connector (C7 family) or a task definition (C8), the parser produces an `ApiActivityNode` instead of a plain `ActivityNode`. Service tasks without one stay plain `ActivityNode`s.

### Model types

| Type | Description |
|---|---|
| `ApiActivityNode` | Extends `ActivityNode`. Adds an `apiHandler` field of type `ApiHandlerDefinition`. |
| `ApiHandlerDefinition` | The API call configuration: `connectorId`, `endpoint`, `method`, `retries`, `taskHeaders`, `inputMappings`, `outputMappings`. |
| `ApiField` | Generic `{ key, value }` pair used for ordered headers and input/output mappings. |

### Camunda 7 / CIB seven / Operaton — `connector`

Follows the Camunda 7 `http-connector` convention (with `operaton:` the element names are the same):

```xml
<bpmn:serviceTask id="SC-PMT" name="Process Payment">
  <bpmn:extensionElements>
    <camunda:properties>
      <camunda:property name="stage"    value="SC" />
      <camunda:property name="activity" value="PMT" />
    </camunda:properties>
    <camunda:connector>
      <camunda:connectorId>http-connector</camunda:connectorId>
      <camunda:inputOutput>
        <camunda:inputParameter name="url">https://api.example.com/v1/charge</camunda:inputParameter>
        <camunda:inputParameter name="method">POST</camunda:inputParameter>
        <camunda:inputParameter name="headers">
          <camunda:map>
            <camunda:entry key="x-api-version">2</camunda:entry>
          </camunda:map>
        </camunda:inputParameter>
        <camunda:inputParameter name="payload">{"amount": 10}</camunda:inputParameter>
        <camunda:outputParameter name="txn_id">${response.transaction_id}</camunda:outputParameter>
      </camunda:inputOutput>
    </camunda:connector>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

| Connector element                     | `ApiHandlerDefinition` field     |
|---------------------------------------|----------------------------------|
| `connectorId`                         | `connectorId`                    |
| input `url`                           | `endpoint`                       |
| input `method`                        | `method`                         |
| input `headers` with `<camunda:map>`  | `taskHeaders` (one per entry)    |
| any other input (e.g. `payload`)      | `inputMappings`                  |
| output parameters                     | `outputMappings`                 |

### Camunda 8 — `taskDefinition`

```xml
<bpmn:serviceTask id="SC-PMT" name="Process Payment">
  <bpmn:extensionElements>
    <zeebe:properties>
      <zeebe:property name="stage"    value="SC" />
      <zeebe:property name="activity" value="PMT" />
    </zeebe:properties>
    <zeebe:taskDefinition type="payment-authorize" retries="3" />
    <zeebe:taskHeaders>
      <zeebe:header key="endpoint" value="https://api.example.com/v1/charge" />
      <zeebe:header key="method"   value="POST" />
    </zeebe:taskHeaders>
    <zeebe:ioMapping>
      <zeebe:input  source="=customer_id" target="customer_id" />
      <zeebe:output source="=txn_id"      target="payment_txn_id" />
    </zeebe:ioMapping>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

`type` → `connectorId`, `retries` → `retries`; the `endpoint` and `method` headers fill their own fields and every other header goes to `taskHeaders`; `ioMapping` inputs and outputs fill `inputMappings` and `outputMappings`.

### Accessing API handler data

```java
workflow.getActivities().forEach(activity -> {
    if (activity instanceof ApiActivityNode api) {
        ApiHandlerDefinition h = api.getApiHandler();
        System.out.printf("%-10s  %s %s%n",
            api.getAbbreviation(), h.getMethod(), h.getEndpoint());

        h.getOutputMappings().forEach(m ->
            System.out.printf("  %s <- %s%n", m.getKey(), m.getValue()));
    }
});
```

### Jackson serialization

`bpmnflow-core` has **no Jackson dependency**. Polymorphic serialization of `ApiActivityNode` is handled by `bpmnflow-spring-boot-starter` (v3.2.x+) via a Jackson MixIn — no annotation leaks into the core library.

Plain `ActivityNode` consumers that do not check for `ApiActivityNode` continue to work without modification.

---

## YAML Configuration

The YAML config controls which properties are extracted and which are required. A missing required property generates an `Inconsistency` in the parsed `Workflow`.

### Camunda 7 config example
```yaml
bpmn_model_parser:

  engine: camunda7

  model_properties:

    participant:
      - name: process_type
        required: true
        extension: true
      - name: process_subtype
        required: true
        extension: true

    process:
      - name: id
        required: true
        extension: false
      - name: versionTag
        required: true
        extension: false
      - name: documentation
        required: true
        extension: false

    lane:
      - name: name
        required: true
        extension: false
      - name: stage
        required: true
        extension: true

    task:
      - name: name
        required: true
        extension: false
      - name: stage
        required: true
        extension: true
      - name: activity
        required: true
        extension: true

    startEvent:
      - name: process_status
        required: true
        extension: true

    endEvent:
      - name: process_status
        required: true
        extension: true

    sequenceFlow:
      - name: name
        required: true
        extension: false
      - name: conclusion
        required: true
        extension: true
```

### Other engines
```yaml
bpmn_model_parser:

  engine: camunda8   # or operaton / cibseven — the only change needed to switch engines

  model_properties:
    # same structure as Camunda 7
    ...
```

Each entry has three fields:

| Field       | Description                                                                   |
|-------------|-------------------------------------------------------------------------------|
| `name`      | Property name — either a standard XML attribute or an extension property      |
| `required`  | If `true`, absence generates an `Inconsistency`                               |
| `extension` | If `true`, read from extension properties; if `false`, from the XML attribute |

> **Note:** The `versionTag` key works for every engine — BPMNFlow knows where to find it based on the `engine` field (attribute for the C7 family, child element for C8).

---

## Parsing Behavior

- **Rules.** Every `WorkflowRule` has both endpoints, except by design: *initial* rules (`RuleType.isInitial()`) have a `null` source and *final* rules (`RuleType.isFinal()`) have a `null` target. The constructor and setters enforce this; rules that would break it are skipped.
- **Deterministic output.** Activities, and the rules derived from them, follow model order, so the same model always produces the same `Workflow`.
- **Multiple pools.** Header data (name, id, version, type, subtype) and stages come from the **first pool that has a process**. Black-box pools are ignored; additional pools with a process are logged as a `WARNING`.
- **Unsupported elements.** Parallel, inclusive, event-based and complex gateways, boundary and intermediate events, sub-processes and call activities are not interpreted yet. They are reported with a `WARNING` log entry (logger `org.bpmnflow.parser.UnsupportedElementHandler`) instead of being ignored silently.
- **Errors.** A missing, malformed or non-BPMN model throws `BpmnModelException`; an invalid YAML throws `BpmnConfigException`. Both are unchecked.

Logging uses `java.util.logging`, so no logging dependency is added to your project.

---

## Upgrading from 3.x

| Change                       | What to do                                                                 |
|------------------------------|----------------------------------------------------------------------------|
| Java baseline 17 → **21**    | Build and run on JDK 21+.                                                  |
| License MIT → **Apache 2.0** | Nothing for users — still free for any use. Versions ≤ 3.2.0 remain MIT.  |
| `BpmnModelException`         | Replaces the generic exception for unreadable models. It extends `RuntimeException`, so existing `catch (RuntimeException e)` still works. |
| Rules with invalid endpoints | No longer produced — code that guarded against an accidental `null` endpoint can rely on `isInitial()` / `isFinal()` instead. |
| Output order                 | Activities, and the rules derived from them, now follow model order (previously hash order). |
| C7 connector mapping         | `headers` given as `<camunda:map>` become `taskHeaders` (one per entry). Other custom input parameters go only to `inputMappings` — they are no longer duplicated into `taskHeaders`. |

See [CHANGELOG.md](CHANGELOG.md) for the full list.

---

## Use Cases

### 1. Model validation in CI/CD

Reject a deployment if the BPMN model doesn't satisfy the config:
```java
Workflow workflow = ModelParser.parser(modelStream, config);

if (!workflow.getInconsistencies().isEmpty()) {
    workflow.getInconsistencies().forEach(i ->
        System.err.println("[" + i.getType() + "] " + i.getDescription())
    );
    System.exit(1);
}
```

### 2. Dynamic next-step resolution

Query the model at runtime instead of hardcoding transitions:
```java
// Which activities are entered when a case reaches a given status?
workflow.getRules().stream()
    .filter(r -> !r.isFinal())
    .filter(r -> myStatus.equals(r.getProcessStatus()))
    .forEach(r -> System.out.println("Entry activity: " + r.getTarget().getAbbreviation()));
```

### 3. Lightweight microservices orchestration

Use the parsed rules to drive event publishing — each activity maps to a Kafka or RabbitMQ topic, and the next step is resolved from the model after each response.

### 4. API-driven service task execution

Use `ApiActivityNode` to execute HTTP calls defined in the BPMN without hardcoding endpoints in Java. The provider pattern in `bpmnflow-process-runtime` selects the right implementation at startup — pure Java (`SpringApiHandlerProvider`), PL/SQL (`PlSqlApiHandlerProvider`), MCP, or Oracle Select AI — while the same `.bpmn` file runs unmodified on any database.

### 5. Self-documenting processes

Generate up-to-date process documentation directly from the BPMN model — activities, stages, conclusions, and transitions are always in sync with the diagram.

### 6. Migrating between engines

Moving from Camunda 7 to Operaton, CIB seven or Camunda 8? Parse the old and new versions of a model and compare the resulting `Workflow` objects to confirm the business structure survived the migration.

---

## What's Next

- Inconsistencies with severity (`ERROR` / `WARNING`) — unsupported elements reported in the `Workflow`, not only in the log
- Support for additional BPMN element types (parallel gateways, sub-processes)
- `PlSqlApiHandlerProvider` — execute API calls via `UTL_HTTP` on Oracle 19c+
- `SelectAiApiHandlerProvider` — `DBMS_CLOUD_AI_AGENT` on Oracle 26ai Autonomous
- Streaming-based parsing for large models
- Optional result caching
- Improved mapping of extracted properties to custom Java objects

Want to help? See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

Copyright 2025-2026 Jeferson Ferreira.

Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE).

Versions up to and including 3.2.0 were released under the MIT License.
