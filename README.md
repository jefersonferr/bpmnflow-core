# BPMNFlow

> Lightweight BPMN model parser for model-driven workflow automation

![Java](https://img.shields.io/badge/Java-17-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-orange)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![BPMN Support](https://img.shields.io/badge/BPMN-2.0-brightgreen)
![CI](https://github.com/jefersonferr/bpmnflow-core/actions/workflows/ci.yml/badge.svg)

---

## Table of Contents

- [Why BPMNFlow?](#why-bpmnflow)
- [BPMNFlow vs Traditional BPMN Engines](#bpmnflow-vs-traditional-bpmn-engines)
- [Quick Start](#quick-start)
- [Extension Properties](#extension-properties)
- [YAML Configuration](#yaml-configuration)
- [Use Cases](#use-cases)
- [What's Next](#whats-next)

---

## Why BPMNFlow?

Most projects that adopt BPMN don't need a full workflow engine — they need to **read business intent from a diagram and act on it**. BPMNFlow solves exactly that, without the infrastructure overhead of Camunda or Flowable.

- **Lightweight** — no runtime engine, no state persistence, no database
- **Model-as-Code** — BPMN becomes a dynamic configuration layer
- **YAML-driven validation** — define which properties are required per element type
- **Zero lock-in** — works with any architecture or framework
- **Spring Boot ready** — see [bpmnflow-spring-boot-starter](https://github.com/jefersonferr/bpmnflow-spring-boot-starter)

---

## BPMNFlow vs Traditional BPMN Engines

| Feature              | BPMNFlow      | Camunda / Flowable |
|----------------------|---------------|--------------------|
| Runtime engine       | No            | Yes                |
| State management     | No            | Yes                |
| Lightweight          | Yes           | No                 |
| Cloud-native         | High          | Medium             |
| Model parsing        | Yes           | Yes                |
| Setup complexity     | Low           | High               |

Use BPMNFlow when you need **interpretation**, not orchestration.

---

## Quick Start

### Installation
```xml
<dependency>
    <groupId>org.bpmnflow</groupId>
    <artifactId>bpmnflow-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Basic Usage
```java
import org.bpmnflow.parser.ModelParser;
import org.bpmnflow.model.Workflow;

try (InputStream model  = Files.newInputStream(Path.of("process.bpmn"));
InputStream config = Files.newInputStream(Path.of("bpmn-config.yaml"))) {

Workflow workflow = ModelParser.parser(model, config);

    System.out.println("Name:        " + workflow.getName());
        System.out.println("Valid:       " + workflow.getInconsistencies().isEmpty());
        System.out.println("Activities:  " + workflow.getActivities().size());
        System.out.println("Rules:       " + workflow.getRules().size());
        }
```

---

## Extension Properties

Extension properties let you embed custom metadata directly in BPMN elements. BPMNFlow reads these properties and maps them to the parsed `Workflow` object.

> **The properties listed below are not fixed** — they are simply the ones extracted and validated based on what you define in your `bpmn-config.yaml`. You have full control over which properties are read, which are required, and which elements they apply to. See [YAML Configuration](#yaml-configuration) for details.

### Supported elements and properties

| Element         | Property          | Description                                        |
|-----------------|-------------------|----------------------------------------------------|
| `Participant`   | `process_type`    | Process classification — any value defined in your model |
| `Participant`   | `process_subtype` | Process subtype — any value defined in your model  |
| `Lane`          | `stage`           | Stage code for the lane                            |
| `Task`          | `stage`           | Stage the task belongs to                          |
| `Task`          | `activity`        | Activity code within the stage                     |
| `StartEvent`    | `process_status`  | Initial process status                             |
| `EndEvent`      | `process_status`  | Final process status                               |
| `SequenceFlow`  | `conclusion`      | Conclusion code that triggers this path            |
| `SequenceFlow`  | `process_status`  | Resulting status after the transition              |

### Defining properties in Camunda Modeler

Open any element → **Properties Panel** → **Extension Properties** → click **+**.

### Defining properties in BPMN XML
```xml
<bpmn:task id="Task_1" name="My Task">
  <bpmn:extensionElements>
    <camunda:properties>
      <camunda:property name="stage"    value="YOUR_STAGE_CODE" />
      <camunda:property name="activity" value="YOUR_ACTIVITY_CODE" />
    </camunda:properties>
  </bpmn:extensionElements>
</bpmn:task>

<bpmn:sequenceFlow id="Flow_1" sourceRef="Task_1" targetRef="Task_2" name="Approved">
  <bpmn:extensionElements>
    <camunda:properties>
      <camunda:property name="conclusion"     value="YOUR_CONCLUSION_CODE" />
      <camunda:property name="process_status" value="YOUR_STATUS" />
    </camunda:properties>
  </bpmn:extensionElements>
</bpmn:sequenceFlow>
```

---

## YAML Configuration

The YAML config controls which properties are extracted and which are required. A missing required property generates an `Inconsistency` in the parsed `Workflow`.
```yaml
bpmn_model_parser:

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
      - name: camunda:versionTag
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

Each entry has three fields:

| Field       | Description                                                              |
|-------------|--------------------------------------------------------------------------|
| `name`      | Property name — either a standard XML attribute or an extension property |
| `required`  | If `true`, absence generates an `Inconsistency`                          |
| `extension` | If `true`, read from `<camunda:properties>`; if `false`, from the XML attribute |

---

## Use Cases

### 1. Model validation in CI/CD

Reject a deployment if the BPMN model doesn't satisfy the config:
```java
Workflow workflow = ModelParser.parser(modelStream, configStream);

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
// Which rules are triggered when a case enters a given status?
workflow.getRules().stream()
    .filter(r -> myStatus.equals(r.getProcessStatus()))
        .forEach(r -> System.out.println("Entry activity: " + r.getTarget().getAbbreviation()));
```

### 3. Lightweight microservices orchestration

Use the parsed rules to drive event publishing — each activity maps to a Kafka or RabbitMQ topic, and the next step is resolved from the model after each response.

### 4. Self-documenting processes

Generate up-to-date process documentation directly from the BPMN model — activities, stages, conclusions, and transitions are always in sync with the diagram.

---

## What's Next

- Support for additional BPMN element types (parallel gateways, subprocesses)
- Streaming-based parsing for large models
- Optional result caching
- Improved mapping of extracted properties to custom Java objects

Want to help? See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

MIT — see [LICENSE](LICENSE).