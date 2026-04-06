package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.Bpmn;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.Workflow;
import org.bpmnflow.parser.engine.EngineAdapterFactory;

import java.io.InputStream;
import java.util.List;

/**
 * Fachada pública para o parsing de modelos BPMN.
 *
 * <p>Esta classe é intencionalmente fina: lê o stream do modelo, monta o
 * {@link ParsingContext} com o {@link org.bpmnflow.parser.engine.EngineAdapter}
 * correto, e delega cada preocupação de parsing a um {@link ElementHandler}
 * dedicado. Não contém lógica de parsing própria.</p>
 *
 * <h2>Ordem de execução dos handlers</h2>
 * <ol>
 *   <li>{@link ParticipantHandler} — cabeçalho do workflow, lanes → stages</li>
 *   <li>{@link FlowNodeHandler}    — tasks, eventos → nodeMap</li>
 *   <li>{@link GatewayHandler}     — exclusive gateways → conclusionMap</li>
 *   <li>{@link RuleHandler}        — sequence flows → WorkflowRules</li>
 * </ol>
 */
public class ModelParser {

    /**
     * Carrega o config do filesystem e delega para a sobrecarga principal.
     * Mantido por backward compatibility com a API pública existente.
     */
    public static Workflow parser(InputStream modelStream, String externalConfigPath) {
        return parser(modelStream, ConfigLoader.loadConfig(externalConfigPath));
    }

    /**
     * Ponto de entrada principal.
     *
     * @param modelStream stream do modelo BPMN (não fechado por este método)
     * @param config      configuração pré-carregada
     * @return um {@link Workflow} completamente populado, possivelmente com inconsistências
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

    // ── Helpers privados ───────────────────────────────────────────────

    private static ParsingContext buildContext(InputStream modelStream,
                                               BpmnPropertiesConfig config) {
        ParsingContext ctx = new ParsingContext();
        ctx.bpmnProperties = new BpmnPropertiesLoader(config);
        ctx.engineAdapter  = EngineAdapterFactory.create(config);
        try {
            ctx.modelInstance = Bpmn.readModelFromStream(modelStream);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao parsear o stream do modelo BPMN", e);
        }
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
                .filter(n -> n instanceof ActivityNode)
                .map(n -> (ActivityNode) n)
                .forEach(workflow::addActivity);
        ctx.rules.forEach(workflow::addRule);
        return workflow;
    }
}
