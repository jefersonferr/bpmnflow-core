package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.Collection;
import java.util.Map;

import static org.bpmnflow.model.InconsistencyCode.*;

/**
 * Trata elementos {@link ExclusiveGateway} no papel de Split.
 *
 * <p>Popula: conclusionMap no {@link ParsingContext} e associa
 * objetos {@link Conclusion} ao {@link ActivityNode} correspondente.</p>
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
        Collection<SequenceFlow> incomings = gateway.getIncoming();
        if (incomings.size() != 1) return;

        SequenceFlow incomingEdge = incomings.iterator().next();
        String sourceId = incomingEdge.getSource().getAttributeValue("id");
        Node rawNode = ctx.getNode(sourceId);

        if (!(rawNode instanceof ActivityNode activityNode)) return;

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
