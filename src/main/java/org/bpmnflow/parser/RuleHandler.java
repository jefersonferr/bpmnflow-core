package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.bpmnflow.model.*;

import java.util.Collection;

import static org.bpmnflow.model.RuleType.*;

/**
 * Trata elementos {@link SequenceFlow} e deriva objetos {@link WorkflowRule}
 * a partir dos padrões de topologia definidos em {@link org.bpmnflow.model.RuleType}.
 *
 * <p>Deve rodar por último — após {@link FlowNodeHandler} (nodeMap) e
 * {@link GatewayHandler} (conclusionMap) estarem completos.</p>
 */
public class RuleHandler implements ElementHandler {

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
        FlowNode source      = flow.getSource();
        FlowNode target      = flow.getTarget();
        Conclusion conclusion = ctx.getConclusion(flow);
        String processStatus = AttributeExtractor.extractOne(
                flow, "process_status", ctx.engineAdapter);

        // Regra 1 — StartEvent → Task
        if (source instanceof StartEvent && target instanceof Task) {
            String startStatus = AttributeExtractor.extractOne(
                    source, "process_status", ctx.engineAdapter);
            ActivityNode targetNode = toActivity(ctx.getNode(id(target)));
            if (notBlank(startStatus)) {
                ctx.addRule(new WorkflowRule(START_TO_TASK, null, targetNode,
                        conclusion, startStatus));
            }
        }

        // Regra 2 — Task → Task
        if (source instanceof Task && target instanceof Task) {
            ActivityNode src = toActivity(ctx.getNode(id(source)));
            ActivityNode tgt = toActivity(ctx.getNode(id(target)));
            ctx.addRule(new WorkflowRule(TASK_TO_TASK, src, tgt, conclusion, processStatus));
        }

        // Task → ExclusiveGateway
        if (source instanceof Task && target instanceof ExclusiveGateway merge) {
            Collection<SequenceFlow> outgoings = merge.getOutgoing();
            if (outgoings.size() == 1) {
                SequenceFlow outgoing = outgoings.iterator().next();
                FlowNode ruleTarget  = outgoing.getTarget();
                ActivityNode src     = toActivity(ctx.getNode(id(source)));

                // Regra 3 — Task → Merge → EndEvent
                if (ruleTarget instanceof EndEvent) {
                    String endStatus = AttributeExtractor.extractOne(
                            ruleTarget, "process_status", ctx.engineAdapter);
                    if (notBlank(endStatus)) {
                        ctx.addRule(new WorkflowRule(TASK_TO_MERGE_TO_END, src, null,
                                conclusion, endStatus));
                    }
                }

                // Regra 4 — Task → Merge → Task
                if (ruleTarget instanceof Task) {
                    ActivityNode tgt = toActivity(ctx.getNode(id(ruleTarget)));
                    ctx.addRule(new WorkflowRule(TASK_TO_MERGE_TO_TASK, src, tgt,
                            conclusion, processStatus));
                }
            }
        }

        // Regra 5 — ExclusiveGateway(split) → Task
        if (source instanceof ExclusiveGateway split && target instanceof Task) {
            Collection<SequenceFlow> incomings = split.getIncoming();
            if (incomings.size() == 1) {
                FlowNode ruleSource = incomings.iterator().next().getSource();
                ActivityNode src    = toActivity(ctx.getNode(id(ruleSource)));
                ActivityNode tgt    = toActivity(ctx.getNode(id(target)));
                if (conclusion != null) {
                    ctx.addRule(new WorkflowRule(SPLIT_TO_TASK, src, tgt,
                            conclusion, processStatus));
                }
            }
        }

        // Regra 6 — Split → Merge
        if (source instanceof ExclusiveGateway splitGw
                && target instanceof ExclusiveGateway mergeGw) {
            boolean isSplit = splitGw.getIncoming().size() == 1;
            boolean isMerge = mergeGw.getOutgoing().size() == 1;
            if (isSplit && isMerge) {
                FlowNode ruleSource = splitGw.getIncoming().iterator().next().getSource();
                FlowNode ruleTarget = mergeGw.getOutgoing().iterator().next().getTarget();
                ActivityNode src    = toActivity(ctx.getNode(id(ruleSource)));
                ActivityNode tgt    = toActivity(ctx.getNode(id(ruleTarget)));
                ctx.addRule(new WorkflowRule(SPLIT_TO_MERGE, src, tgt,
                        conclusion, processStatus));
            }
        }

        // Regra 7 — Task → EndEvent
        if (source instanceof Task && target instanceof EndEvent) {
            String endStatus = AttributeExtractor.extractOne(
                    target, "process_status", ctx.engineAdapter);
            ActivityNode src = toActivity(ctx.getNode(id(source)));
            if (notBlank(endStatus)) {
                ctx.addRule(new WorkflowRule(TASK_TO_END, src, null,
                        conclusion, endStatus));
            }
        }

        // Regra 8 — Task → Split → EndEvent
        if (source instanceof ExclusiveGateway splitGw && target instanceof EndEvent) {
            Collection<SequenceFlow> incomings = splitGw.getIncoming();
            if (incomings.size() == 1) {
                FlowNode ruleSource = incomings.iterator().next().getSource();
                if (ruleSource instanceof Task) {
                    String endStatus = AttributeExtractor.extractOne(
                            target, "process_status", ctx.engineAdapter);
                    ActivityNode src = toActivity(ctx.getNode(id(ruleSource)));
                    if (notBlank(endStatus) && conclusion != null) {
                        ctx.addRule(new WorkflowRule(TASK_TO_SPLIT_TO_END, src, null,
                                conclusion, endStatus));
                    }
                }
            }
        }
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
}
