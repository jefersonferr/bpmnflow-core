package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.bpmnflow.model.InconsistencyCode.*;

/**
 * Handles {@link ExclusiveGateway} elements in their Split role
 * (any gateway with 2 or more outgoing edges).
 *
 * <p>A gateway is treated as a <em>pure merge</em> when it has fewer than 2 outgoing
 * edges (i.e. it only converges flows) and is skipped — those are handled by
 * {@link RuleHandler}. Any gateway with 2+ outgoing edges is a split or merge+split
 * and must have its outgoing flows validated and conclusions registered.</p>
 *
 * <p>This correctly handles the <em>merge+split</em> pattern where a gateway has
 * multiple incoming edges (e.g. from both a primary task and a retry task) but
 * still fans out with labelled conclusions. The retry/merge edge (whose source is
 * also a task) contributes no conclusion and must not block resolution of the
 * primary edge.</p>
 *
 * <p>Resolution rule: among all incoming edges, collect <strong>all</strong>
 * {@link ActivityNode} predecessors. Conclusions are registered for every
 * predecessor so that activities sharing a merge+split gateway (e.g. a primary
 * task and a retry task both feeding the same gateway) each receive the full
 * set of conclusions from the gateway's outgoing flows.</p>
 *
 * <p>Must run after {@link FlowNodeHandler} because it reads the nodeMap.</p>
 */
public class GatewayHandler implements ElementHandler {

    @Override
    public void handle(ParsingContext ctx) {
        ModelElementType gatewayType =
                ctx.modelInstance.getModel().getType(ExclusiveGateway.class);
        Collection<ModelElementInstance> gateways =
                ctx.modelInstance.getModelElementsByType(gatewayType);

        for (ModelElementInstance node : gateways) {
            if (node instanceof ExclusiveGateway gateway) {
                handleGateway(gateway, ctx);
            }
        }
    }

    private void handleGateway(ExclusiveGateway gateway, ParsingContext ctx) {
        // A pure-merge gateway has exactly one outgoing edge (it only converges flows
        // and does not fan out). Those are handled by RuleHandler — skip here.
        // Any gateway with 2+ outgoing edges is a split (or merge+split) and must
        // have its outgoing flows validated and its conclusions registered.
        if (gateway.getOutgoing().size() < 2) return;

        // Collect ALL ActivityNode predecessors among all incoming edges.
        // In a merge+split pattern (e.g. SC-RCV and SC-CLM both feeding the same
        // gateway) every predecessor must receive the full set of conclusions so
        // that the runtime can advance from any of them using a valid conclusion code.
        List<ActivityNode> predecessors = new ArrayList<>();
        for (SequenceFlow incomingEdge : gateway.getIncoming()) {
            String sourceId = incomingEdge.getSource().getAttributeValue("id");
            Node rawNode = ctx.getNode(sourceId);
            if (rawNode instanceof ActivityNode candidate) {
                predecessors.add(candidate);
            }
        }

        if (predecessors.isEmpty()) return; // no task predecessor — skip

        for (SequenceFlow outgoing : gateway.getOutgoing()) {
            for (ActivityNode predecessor : predecessors) {
                handleOutgoingFlow(outgoing, predecessor, ctx);
            }
        }
    }

    private void handleOutgoingFlow(SequenceFlow flow, ActivityNode source,
                                    ParsingContext ctx) {
        String conclusionName = flow.getName();
        Map<String, String> attrs = AttributeExtractor.extract(flow, ctx.engineAdapter);
        String conclusionCode = attrs.get("conclusion");
        boolean valid = true;

        if (ctx.bpmnProperties.getSequenceFlow("name").isRequired()
                && isBlank(conclusionName)) {
            ctx.addInconsistency(SEQUENCE_FLOW_NAME_MISSING.of(flow.getId()));
            valid = false;
        }

        if (ctx.bpmnProperties.getSequenceFlow("conclusion").isRequired()
                && isBlank(conclusionCode)) {
            ctx.addInconsistency(SEQUENCE_FLOW_CONCLUSION_MISSING.of(flow.getId()));
            valid = false;
        }

        if (valid) {
            Conclusion conclusion = new Conclusion(conclusionCode, conclusionName);
            source.addConclusion(conclusion);
            ctx.putConclusion(flow, conclusion);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}