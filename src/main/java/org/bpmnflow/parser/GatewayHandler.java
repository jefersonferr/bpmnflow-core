package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.Collection;
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
 * <p>Resolution rule: among all incoming edges, pick the <strong>first</strong> one
 * whose source is an {@link ActivityNode}. If no such edge exists the gateway is
 * skipped.</p>
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

        // Find the primary ActivityNode predecessor among all incoming edges.
        // In a merge+split pattern there may be several incoming edges; we pick
        // the first one whose direct source resolves to an ActivityNode.
        ActivityNode activityNode = null;
        for (SequenceFlow incomingEdge : gateway.getIncoming()) {
            String sourceId = incomingEdge.getSource().getAttributeValue("id");
            Node rawNode = ctx.getNode(sourceId);
            if (rawNode instanceof ActivityNode candidate) {
                activityNode = candidate;
                break;
            }
        }

        if (activityNode == null) return; // no task predecessor — skip

        for (SequenceFlow outgoing : gateway.getOutgoing()) {
            handleOutgoingFlow(outgoing, activityNode, ctx);
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