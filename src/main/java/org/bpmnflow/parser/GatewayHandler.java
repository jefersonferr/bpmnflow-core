package org.bpmnflow.parser;

import org.bpmnflow.model.*;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import java.util.Collection;
import java.util.Map;

import static org.bpmnflow.model.InconsistencyCode.*;

/**
 * Handles {@link ExclusiveGateway} elements in their Split role
 * (exactly one incoming edge = the gateway fans out to multiple paths).
 *
 * <p>Populates: conclusionMap in {@link ParsingContext} and attaches
 * {@link Conclusion} objects to the corresponding {@link ActivityNode}.</p>
 *
 * <p>Must run after {@link FlowNodeHandler} because it reads the nodeMap.</p>
 */
public class GatewayHandler implements ElementHandler {

    @Override
    public void handle(ParsingContext ctx) {
        ModelElementType gatewayType = ctx.modelInstance.getModel().getType(ExclusiveGateway.class);
        Collection<ModelElementInstance> gateways = ctx.modelInstance.getModelElementsByType(gatewayType);

        for (ModelElementInstance node : gateways) {
            if (node instanceof ExclusiveGateway gateway) {
                handleGateway(gateway, ctx);
            }
        }
    }

    private void handleGateway(ExclusiveGateway gateway, ParsingContext ctx) {
        Collection<SequenceFlow> incomings = gateway.getIncoming();
        if (incomings.size() != 1) return; // not a split — skip (merge is handled by RuleHandler)

        SequenceFlow incomingEdge = incomings.iterator().next();
        String sourceId = incomingEdge.getSource().getAttributeValue("id");
        Node rawNode = ctx.getNode(sourceId);

        if (!(rawNode instanceof ActivityNode activityNode)) return;

        for (SequenceFlow outgoing : gateway.getOutgoing()) {
            handleOutgoingFlow(outgoing, activityNode, ctx);
        }
    }

    private void handleOutgoingFlow(SequenceFlow flow, ActivityNode source, ParsingContext ctx) {
        String conclusionName = flow.getName();
        Map<String, String> attrs = AttributeExtractor.extract(flow);
        String conclusionCode = attrs.get("conclusion");
        boolean valid = true;

        if (ctx.bpmnProperties.getSequenceFlow("name").isRequired() && isBlank(conclusionName)) {
            ctx.addInconsistency(SEQUENCE_FLOW_NAME_MISSING.of(flow.getId()));
            valid = false;
        }

        if (ctx.bpmnProperties.getSequenceFlow("conclusion").isRequired() && isBlank(conclusionCode)) {
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
