package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.SequenceFlow;
import org.bpmnflow.model.*;
import org.bpmnflow.parser.engine.EngineAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acumulador mutável compartilhado por todos os {@link ElementHandler}
 * durante uma única passagem de parsing.
 *
 * <p>O {@link ModelParser} cria uma instância por chamada, injeta o
 * {@link EngineAdapter} correto, e lê o estado final para construir o
 * {@link Workflow}. Nenhum handler guarda referência a outro handler —
 * comunicam-se apenas através deste objeto.</p>
 */
public class ParsingContext {

    // ── Cabeçalho do Workflow ──────────────────────────────────────────
    String workflowName;
    String workflowId;
    String workflowVersion;
    String workflowDocumentation;
    String processType;
    String processSubtype;

    // ── Elementos coletados ────────────────────────────────────────────
    final List<Stage>                   stages          = new ArrayList<>();
    final List<Inconsistency>           inconsistencies = new ArrayList<>();
    final List<WorkflowRule>            rules           = new ArrayList<>();
    final Map<String, Node>             nodeMap         = new HashMap<>();
    final Map<SequenceFlow, Conclusion> conclusionMap   = new HashMap<>();

    // ── Modelo parseado (definido uma vez pelo ModelParser) ───────────
    BpmnModelInstance modelInstance;

    // ── Acessor de config (definido uma vez pelo ModelParser) ─────────
    BpmnPropertiesLoader bpmnProperties;

    // ── Adapter do engine ativo (injetado pelo ModelParser) ───────────
    EngineAdapter engineAdapter;

    // ── Mutadores de conveniência ──────────────────────────────────────

    public void addInconsistency(Inconsistency inconsistency) {
        inconsistencies.add(inconsistency);
    }

    public void addStage(Stage stage) {
        stages.add(stage);
    }

    public void addRule(WorkflowRule rule) {
        rules.add(rule);
    }

    public void putNode(String id, Node node) {
        nodeMap.put(id, node);
    }

    public Node getNode(String id) {
        return nodeMap.get(id);
    }

    public void putConclusion(SequenceFlow flow, Conclusion conclusion) {
        conclusionMap.put(flow, conclusion);
    }

    public Conclusion getConclusion(SequenceFlow flow) {
        return conclusionMap.get(flow);
    }
}
