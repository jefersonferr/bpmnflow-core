package org.bpmnflow.parser;

/**
 * Contract for each parsing phase.
 *
 * <p>Each implementation is responsible for exactly one category of BPMN
 * elements. Handlers are stateless — all mutable state lives in
 * {@link ParsingContext}.</p>
 *
 * <p>Execution order matters: {@link ParticipantHandler} must run before
 * {@link FlowNodeHandler}, and both must complete before {@link GatewayHandler}
 * (which reads the nodeMap populated by FlowNodeHandler). {@link RuleHandler}
 * runs last, after the conclusionMap is built by GatewayHandler.</p>
 */
public interface ElementHandler {

    /**
     * Processes the relevant BPMN elements and writes results into {@code ctx}.
     *
     * @param ctx the shared parsing accumulator for this parse pass
     */
    void handle(ParsingContext ctx);
}
