package org.bpmnflow.parser;

import org.bpmnflow.model.*;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import java.util.*;

/**
 * Mutable accumulator shared across all {@link ElementHandler} implementations
 * during a single parsing pass.
 *
 * <p>ModelParser creates one instance per call, passes it to every handler, and
 * reads the final state to build the {@link Workflow}. No handler holds a
 * reference to another handler — they communicate only through this object.</p>
 */
public class ParsingContext {

    // -- Workflow header fields --
    String workflowName;
    String workflowId;
    String workflowVersion;
    String workflowDocumentation;
    String processType;
    String processSubtype;

    // -- Collected elements --
    final List<Stage>          stages          = new ArrayList<>();
    final List<Inconsistency>  inconsistencies = new ArrayList<>();
    final Map<String, Node>    nodeMap         = new HashMap<>();
    final Map<org.camunda.bpm.model.bpmn.instance.SequenceFlow, Conclusion> conclusionMap = new HashMap<>();

    // -- Parsed model (set once by ModelParser before handlers run) --
    BpmnModelInstance modelInstance;

    // -- Config accessor (set once by ModelParser) --
    BpmnPropertiesLoader bpmnProperties;

    // ---------------------------------------------------------------
    // Convenience mutators (keep handler code readable)
    // ---------------------------------------------------------------

    public void addInconsistency(Inconsistency inconsistency) {
        inconsistencies.add(inconsistency);
    }

    public void addStage(Stage stage) {
        stages.add(stage);
    }

    public void putNode(String id, Node node) {
        nodeMap.put(id, node);
    }

    public Node getNode(String id) {
        return nodeMap.get(id);
    }

    public void putConclusion(org.camunda.bpm.model.bpmn.instance.SequenceFlow flow, Conclusion conclusion) {
        conclusionMap.put(flow, conclusion);
    }

    public Conclusion getConclusion(org.camunda.bpm.model.bpmn.instance.SequenceFlow flow) {
        return conclusionMap.get(flow);
    }
}
