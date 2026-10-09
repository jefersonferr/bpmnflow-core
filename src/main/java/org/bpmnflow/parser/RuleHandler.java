package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static org.bpmnflow.model.RuleType.*;

/**
 * Handles {@link SequenceFlow} elements and derives {@link WorkflowRule} objects
 * from the topology patterns defined in {@link org.bpmnflow.model.RuleType}.
 *
 * <p>Must run last — after {@link FlowNodeHandler} (nodeMap) and
 * {@link GatewayHandler} (conclusionMap) are both complete.</p>
 */
public class RuleHandler implements ElementHandler {

    private static final Logger LOGGER = Logger.getLogger(RuleHandler.class.getName());

    @Override
    public void handle(ParsingContext ctx) {
        ModelElementType sequenceFlowType =
                ctx.modelInstance.getModel().getType(SequenceFlow.class);
        Collection<ModelElementInstance> edges =
                ctx.modelInstance.getModelElementsByType(sequenceFlowType);

        for (ModelElementInstance edge : edges) {
            applyRules((SequenceFlow) edge, ctx);
        }
    }

    private void applyRules(SequenceFlow flow, ParsingContext ctx) {
        FlowNode source       = flow.getSource();
        FlowNode target       = flow.getTarget();
        Conclusion conclusion  = ctx.getConclusion(flow);
        String processStatus  = AttributeExtractor.extractOne(
                flow, "process_status", ctx.engineAdapter);

        // Rule 1 — StartEvent → Task
        if (source instanceof StartEvent && target instanceof Task) {
            String startStatus = AttributeExtractor.extractOne(
                    source, "process_status", ctx.engineAdapter);
            ActivityNode targetNode = toActivity(ctx.getNode(id(target)));
            if (notBlank(startStatus)) {
                addRule(ctx, START_TO_TASK, null, targetNode,
                        conclusion, startStatus);
            }
        }

        // Rule 2 — Task → Task
        if (source instanceof Task && target instanceof Task) {
            ActivityNode src = toActivity(ctx.getNode(id(source)));
            ActivityNode tgt = toActivity(ctx.getNode(id(target)));
            addRule(ctx, TASK_TO_TASK, src, tgt, conclusion, processStatus);
        }

        // Task → ExclusiveGateway (merge or split fanout)
        if (source instanceof Task && target instanceof ExclusiveGateway merge) {
            Collection<SequenceFlow> outgoings = merge.getOutgoing();
            if (outgoings.size() == 1) {
                SequenceFlow outgoing = outgoings.iterator().next();
                FlowNode ruleTarget  = outgoing.getTarget();
                ActivityNode src     = toActivity(ctx.getNode(id(source)));

                // Rule 3 — Task → Merge → EndEvent
                if (ruleTarget instanceof EndEvent) {
                    String endStatus = AttributeExtractor.extractOne(
                            ruleTarget, "process_status", ctx.engineAdapter);
                    if (notBlank(endStatus)) {
                        addRule(ctx, TASK_TO_MERGE_TO_END, src, null,
                                conclusion, endStatus);
                    }
                }

                // Rule 4 — Task → Merge → Task
                if (ruleTarget instanceof Task) {
                    ActivityNode tgt = toActivity(ctx.getNode(id(ruleTarget)));
                    addRule(ctx, TASK_TO_MERGE_TO_TASK, src, tgt,
                            conclusion, processStatus);
                }
            }
        }

        // Rule 5 — ExclusiveGateway(split) → Task
        // Supports merge+split gateways (multiple incomings): resolves ALL
        // ActivityNode predecessors and generates a SPLIT_TO_TASK rule for each,
        // so that every activity sharing the gateway receives the correct transitions.
        if (source instanceof ExclusiveGateway split && target instanceof Task) {
            List<ActivityNode> predecessors = resolveAllActivityPredecessors(split, ctx);
            for (ActivityNode src : predecessors) {
                ActivityNode tgt = toActivity(ctx.getNode(id(target)));
                if (conclusion != null) {
                    addRule(ctx, SPLIT_TO_TASK, src, tgt,
                            conclusion, processStatus);
                }
            }
        }

        // Rule 6 — Split → Merge
        // Supports merge+split gateways (multiple incomings), consistent with
        // Rules 5 and 8: every ActivityNode predecessor of the split receives a
        // SPLIT_TO_MERGE rule towards the activity that follows the merge.
        if (source instanceof ExclusiveGateway splitGw
                && target instanceof ExclusiveGateway mergeGw
                && mergeGw.getOutgoing().size() == 1) {
            FlowNode ruleTarget = mergeGw.getOutgoing().iterator().next().getTarget();
            ActivityNode tgt    = toActivity(ctx.getNode(id(ruleTarget)));
            for (ActivityNode src : resolveAllActivityPredecessors(splitGw, ctx)) {
                addRule(ctx, SPLIT_TO_MERGE, src, tgt, conclusion, processStatus);
            }
        }

        // Rule 7 — Task → EndEvent
        if (source instanceof Task && target instanceof EndEvent) {
            String endStatus = AttributeExtractor.extractOne(
                    target, "process_status", ctx.engineAdapter);
            ActivityNode src = toActivity(ctx.getNode(id(source)));
            if (notBlank(endStatus)) {
                addRule(ctx, TASK_TO_END, src, null,
                        conclusion, endStatus);
            }
        }

        // Rule 8 — Task → Split → EndEvent
        // Supports merge+split gateways: resolves ALL ActivityNode predecessors
        // and generates a TASK_TO_SPLIT_TO_END rule for each.
        if (source instanceof ExclusiveGateway splitGw && target instanceof EndEvent) {
            List<ActivityNode> predecessors = resolveAllActivityPredecessors(splitGw, ctx);
            for (ActivityNode src : predecessors) {
                String endStatus = AttributeExtractor.extractOne(
                        target, "process_status", ctx.engineAdapter);
                if (notBlank(endStatus) && conclusion != null) {
                    addRule(ctx, TASK_TO_SPLIT_TO_END, src, null,
                            conclusion, endStatus);
                }
            }
        }
    }

    /**
     * Single entry point for rule creation.
     *
     * <p>Skips the rule when its endpoints violate the {@link WorkflowRule}
     * invariants — typically because a source or target task failed validation
     * and was never added to the nodeMap. That task already produced its own
     * inconsistency, so the skip is logged at FINE level only.</p>
     */
    private static void addRule(ParsingContext ctx, RuleType type, ActivityNode src,
                                ActivityNode tgt, Conclusion conclusion, String status) {
        if (!WorkflowRule.endpointsValid(type, src, tgt)) {
            LOGGER.fine(() -> "Skipping " + type + " rule: source=" + abbr(src)
                    + ", target=" + abbr(tgt) + " — an endpoint refers to an invalid element");
            return;
        }
        ctx.addRule(new WorkflowRule(type, src, tgt, conclusion, status));
    }

    private static String abbr(ActivityNode node) {
        return node == null ? "null" : node.getAbbreviation();
    }

    private static String id(FlowNode node) {
        return node.getAttributeValue("id");
    }

    private static ActivityNode toActivity(Node node) {
        return (node instanceof ActivityNode a) ? a : null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Resolves ALL {@link ActivityNode} predecessors of a gateway by iterating
     * its incoming edges and collecting every source that maps to an ActivityNode.
     *
     * <p>Used for merge+split gateways where multiple activities (e.g. SC-RCV and
     * SC-CLM) share the same gateway and must each receive the full set of
     * outgoing rules.</p>
     *
     * @return list of all ActivityNode predecessors; empty if none found.
     */
    private static List<ActivityNode> resolveAllActivityPredecessors(
            ExclusiveGateway gateway, ParsingContext ctx) {
        List<ActivityNode> result = new ArrayList<>();
        for (SequenceFlow incoming : gateway.getIncoming()) {
            Node candidate = ctx.getNode(incoming.getSource().getAttributeValue("id"));
            if (candidate instanceof ActivityNode activityNode) {
                result.add(activityNode);
            }
        }
        return result;
    }
}