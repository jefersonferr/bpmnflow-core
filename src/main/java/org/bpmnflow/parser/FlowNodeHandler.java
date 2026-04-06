package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.Collection;
import java.util.Map;

import static org.bpmnflow.model.InconsistencyCode.*;

/**
 * Trata elementos {@link Task}, {@link StartEvent} e {@link EndEvent}.
 *
 * <p>Popula: nodeMap no {@link ParsingContext} com entradas
 * {@link ActivityNode} e {@link StartEndNode}.</p>
 */
public class FlowNodeHandler implements ElementHandler {

    @Override
    public void handle(ParsingContext ctx) {
        ModelElementType flowNodeType =
                ctx.modelInstance.getModel().getType(FlowNode.class);
        Collection<ModelElementInstance> nodes =
                ctx.modelInstance.getModelElementsByType(flowNodeType);

        for (ModelElementInstance node : nodes) {
            FlowNode flowNode = (FlowNode) node;
            if      (flowNode instanceof Task)       handleTask(flowNode, ctx);
            else if (flowNode instanceof StartEvent) handleEvent(flowNode, true,  ctx);
            else if (flowNode instanceof EndEvent)   handleEvent(flowNode, false, ctx);
        }
    }

    private void handleTask(FlowNode flowNode, ParsingContext ctx) {
        String id            = flowNode.getAttributeValue("id");
        String name          = flowNode.getAttributeValue("name");
        String documentation = flowNode.getAttributeValue("documentation");
        Map<String, String> attrs = AttributeExtractor.extract(flowNode, ctx.engineAdapter);

        boolean valid = true;

        if (ctx.bpmnProperties.getTask("name").isRequired() && isBlank(name)) {
            ctx.addInconsistency(TASK_NAME_MISSING.of(id));
            valid = false;
        }

        String stageCode = attrs.get("stage");
        if (ctx.bpmnProperties.getTask("stage").isRequired() && isBlank(stageCode)) {
            ctx.addInconsistency(TASK_STAGE_MISSING.of(id));
            valid = false;
        }

        String activityCode = attrs.get("activity");
        if (ctx.bpmnProperties.getTask("activity").isRequired() && isBlank(activityCode)) {
            ctx.addInconsistency(TASK_ACTIVITY_MISSING.of(id));
            valid = false;
        }

        if (valid) {
            ctx.putNode(id, new ActivityNode(stageCode, activityCode, name, documentation));
        }
    }

    private void handleEvent(FlowNode flowNode, boolean isStart, ParsingContext ctx) {
        String id            = flowNode.getAttributeValue("id");
        String name          = flowNode.getAttributeValue("name");
        String documentation = flowNode.getAttributeValue("documentation");
        String processStatus = AttributeExtractor.extractOne(
                flowNode, "process_status", ctx.engineAdapter);

        var requiredCheck = isStart
                ? ctx.bpmnProperties.getStartEvent("process_status")
                : ctx.bpmnProperties.getEndEvent("process_status");

        if (requiredCheck.isRequired() && isBlank(processStatus)) {
            ctx.addInconsistency(EVENT_PROCESS_STATUS_MISSING.of(id));
        }

        ctx.putNode(id, new StartEndNode(name, documentation, processStatus));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
