package org.bpmnflow.parser;

import org.bpmnflow.model.Workflow;
import org.camunda.bpm.model.bpmn.Bpmn;

import java.io.InputStream;
import java.util.List;

/**
 * Public façade for BPMN model parsing.
 *
 * <p>This class is intentionally thin: it reads the model stream, wires up
 * the {@link ParsingContext}, and delegates every parsing concern to a
 * dedicated {@link ElementHandler}. It contains zero parsing logic of its own.</p>
 *
 * <h2>Handler execution order</h2>
 * <ol>
 *   <li>{@link ParticipantHandler} — workflow header, lanes → stages</li>
 *   <li>{@link FlowNodeHandler}    — tasks, start/end events → nodeMap</li>
 *   <li>{@link GatewayHandler}     — exclusive gateways → conclusionMap</li>
 *   <li>{@link RuleHandler}        — sequence flows → WorkflowRules</li>
 * </ol>
 *
 * <p>Adding support for a new element type (e.g. parallel gateways, sub-processes)
 * means implementing a new {@link ElementHandler} and inserting it at the
 * correct position in the list below — no changes to existing classes required.</p>
 */
public class ModelParser {

    /**
     * Loads the config from the filesystem path and delegates to the main overload.
     * Retained for backward compatibility with the existing public API.
     */
    public static Workflow parser(InputStream modelStream, String externalConfigPath) {
        return parser(modelStream, ConfigLoader.loadConfig(externalConfigPath));
    }

    /**
     * Main entry point. Preferred when the config is loaded from the classpath
     * (tests, Spring Boot, embedded JARs).
     *
     * @param modelStream BPMN model as an input stream (not closed by this method)
     * @param config      pre-loaded properties configuration
     * @return a fully populated {@link Workflow}, possibly with inconsistencies
     * @throws RuntimeException if the stream cannot be parsed as valid BPMN
     */
    public static Workflow parser(InputStream modelStream, BpmnPropertiesConfig config) {
        ParsingContext ctx = buildContext(modelStream, config);

        List<ElementHandler> handlers = List.of(
                new ParticipantHandler(),
                new FlowNodeHandler(),
                new GatewayHandler(),
                new RuleHandler()
        );

        for (ElementHandler handler : handlers) {
            handler.handle(ctx);
        }

        return buildWorkflow(ctx);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private static ParsingContext buildContext(InputStream modelStream, BpmnPropertiesConfig config) {
        ParsingContext ctx = new ParsingContext();
        ctx.bpmnProperties = new BpmnPropertiesLoader(config);
        try {
            ctx.modelInstance = Bpmn.readModelFromStream(modelStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BPMN model stream", e);
        }
        ctx.workflow = new Workflow(null, null, null, null, null, null);
        return ctx;
    }

    private static Workflow buildWorkflow(ParsingContext ctx) {
        Workflow workflow = new Workflow(
                ctx.workflowName,
                ctx.workflowId,
                ctx.workflowVersion,
                ctx.workflowDocumentation,
                ctx.processType,
                ctx.processSubtype
        );
        workflow.setStages(ctx.stages);
        workflow.setInconsistencies(ctx.inconsistencies);
        ctx.nodeMap.values().stream()
                .filter(n -> n instanceof org.bpmnflow.model.ActivityNode)
                .map(n -> (org.bpmnflow.model.ActivityNode) n)
                .forEach(workflow::addActivity);
        ctx.rules.forEach(workflow::addRule);
        return workflow;
    }
}
