package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.Activity;
import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.BoundaryEvent;
import io.camunda.zeebe.model.bpmn.instance.CallActivity;
import io.camunda.zeebe.model.bpmn.instance.ComplexGateway;
import io.camunda.zeebe.model.bpmn.instance.EventBasedGateway;
import io.camunda.zeebe.model.bpmn.instance.InclusiveGateway;
import io.camunda.zeebe.model.bpmn.instance.IntermediateCatchEvent;
import io.camunda.zeebe.model.bpmn.instance.IntermediateThrowEvent;
import io.camunda.zeebe.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import io.camunda.zeebe.model.bpmn.instance.ParallelGateway;
import io.camunda.zeebe.model.bpmn.instance.SubProcess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Detects BPMN elements that BPMNFlow does not interpret and reports them
 * with a WARNING log entry, so they are no longer ignored silently.
 *
 * <p>Transitions that pass through these elements produce no
 * {@link org.bpmnflow.model.WorkflowRule}. The parse result itself is not
 * changed — no {@link org.bpmnflow.model.Inconsistency} is added, so models
 * that are valid today stay valid. A future version will report these as
 * inconsistencies with a WARNING severity.</p>
 *
 * <p>Unsupported elements: parallel, inclusive, event-based and complex
 * gateways; boundary and intermediate events; sub-processes (including
 * transactions, ad-hoc and event sub-processes); call activities; and
 * multi-instance activities.</p>
 */
public class UnsupportedElementHandler implements ElementHandler {

    private static final Logger LOGGER =
            Logger.getLogger(UnsupportedElementHandler.class.getName());

    /** Element types that are reported when present in the model. */
    static final List<Class<? extends BaseElement>> UNSUPPORTED_TYPES = List.of(
            ParallelGateway.class,
            InclusiveGateway.class,
            EventBasedGateway.class,
            ComplexGateway.class,
            BoundaryEvent.class,
            IntermediateCatchEvent.class,
            IntermediateThrowEvent.class,
            SubProcess.class,
            CallActivity.class
    );

    /** Key used for activities that declare multi-instance loop characteristics. */
    static final String MULTI_INSTANCE = "multiInstance";

    @Override
    public void handle(ParsingContext ctx) {
        findUnsupported(ctx.modelInstance).forEach((type, ids) ->
                LOGGER.warning(() -> String.format(
                        "BpmnFlow does not support %d '%s' element(s) %s — they are ignored"
                                + " and no rules are derived through them.",
                        ids.size(), type, ids)));
    }

    /**
     * Collects the unsupported elements of the model.
     *
     * @return element type name → element ids, in a deterministic order;
     *         empty when the model uses only supported elements
     */
    static Map<String, List<String>> findUnsupported(BpmnModelInstance model) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();

        for (Class<? extends BaseElement> type : UNSUPPORTED_TYPES) {
            for (BaseElement element : model.getModelElementsByType(type)) {
                // getModelElementsByType includes subtypes (e.g. Transaction under
                // SubProcess); the concrete type name keeps the report precise.
                if (seen.add(element.getId())) {
                    result.computeIfAbsent(element.getElementType().getTypeName(),
                            k -> new ArrayList<>()).add(element.getId());
                }
            }
        }

        for (Activity activity : model.getModelElementsByType(Activity.class)) {
            if (activity.getLoopCharacteristics() instanceof MultiInstanceLoopCharacteristics) {
                result.computeIfAbsent(MULTI_INSTANCE, k -> new ArrayList<>())
                        .add(activity.getId());
            }
        }
        return result;
    }
}
