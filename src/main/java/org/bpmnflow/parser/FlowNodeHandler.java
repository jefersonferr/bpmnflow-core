package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.Collection;
import java.util.Map;

import static org.bpmnflow.model.InconsistencyCode.*;

/**
 * Handles {@link Task}, {@link ServiceTask}, {@link StartEvent} and
 * {@link EndEvent} elements.
 *
 * <p>Populates: nodeMap in {@link ParsingContext} with {@link ActivityNode},
 * {@link ApiActivityNode} and {@link StartEndNode} entries.</p>
 *
 * <h2>Task vs ServiceTask</h2>
 * <p>{@code ServiceTask} extends {@code Task} in the BPMN meta-model, so both
 * are captured by the {@code instanceof Task} check. The handler then asks the
 * active {@link org.bpmnflow.parser.engine.EngineAdapter} for an
 * {@link ApiHandlerDefinition} via {@code extractApiHandler()}. When a
 * definition is returned (non-null), an {@link ApiActivityNode} is created;
 * otherwise a plain {@link ActivityNode} is created — preserving full
 * backward compatibility for models that contain no ServiceTask.</p>
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
            if (flowNode instanceof ServiceTask) {
                handleServiceTask(id, name, documentation, stageCode, activityCode, flowNode, ctx);
            } else {
                ctx.putNode(id, new ActivityNode(stageCode, activityCode, name, documentation));
            }
        }
    }

    private void handleServiceTask(String id, String name, String documentation,
                                   String stageCode, String activityCode,
                                   FlowNode flowNode, ParsingContext ctx) {

        ApiHandlerDefinition apiHandler =
                ctx.engineAdapter.extractApiHandler(flowNode);

        if (apiHandler == null) {
            // ServiceTask without connector/taskDefinition — treat as plain ActivityNode
            ctx.putNode(id, new ActivityNode(stageCode, activityCode, name, documentation));
            return;
        }

        boolean apiValid = validateApiHandler(id, apiHandler, ctx);

        if (apiValid) {
            ctx.putNode(id, new ApiActivityNode(
                    stageCode, activityCode, name, documentation, apiHandler));
        }
    }

    /**
     * Validates mandatory fields of the extracted {@link ApiHandlerDefinition}.
     *
     * <p>Validation rules are driven by the {@code serviceTask} section of the
     * YAML config — the same pattern used for task/lane/sequenceFlow sections.
     * Fields: {@code connectorId}, {@code endpoint}, {@code method}.</p>
     *
     * @return {@code true} when all required fields are present; {@code false}
     *         when at least one inconsistency was added.
     */
    private boolean validateApiHandler(String id, ApiHandlerDefinition def,
                                       ParsingContext ctx) {
        boolean valid = true;

        if (ctx.bpmnProperties.getServiceTask("connectorId").isRequired()
                && isBlank(def.getConnectorId())) {
            ctx.addInconsistency(API_CONNECTOR_ID_MISSING.of(id));
            valid = false;
        }

        if (ctx.bpmnProperties.getServiceTask("endpoint").isRequired()
                && isBlank(def.getEndpoint())) {
            ctx.addInconsistency(API_ENDPOINT_MISSING.of(id));
            valid = false;
        }

        if (ctx.bpmnProperties.getServiceTask("method").isRequired()
                && isBlank(def.getMethod())) {
            ctx.addInconsistency(API_METHOD_MISSING.of(id));
            valid = false;
        }

        return valid;
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