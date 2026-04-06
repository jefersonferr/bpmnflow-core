package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import org.bpmnflow.parser.engine.EngineAdapter;

import java.util.Map;

/**
 * Fachada de conveniência para extração de extension properties de elementos BPMN.
 *
 * <p>Toda a lógica de extração foi movida para as implementações de
 * {@link EngineAdapter}. Esta classe é apenas um ponto de acesso estático
 * que os handlers usam para manter o código conciso.</p>
 */
public final class AttributeExtractor {

    private AttributeExtractor() {}

    /**
     * Extrai todas as extension properties do elemento, delegando ao adapter ativo.
     *
     * @param element o elemento BPMN a inspecionar
     * @param adapter o adapter do engine configurado
     * @return mapa nome→valor; nunca null
     */
    public static Map<String, String> extract(BaseElement element, EngineAdapter adapter) {
        return adapter.extractProperties(element);
    }

    /**
     * Retorna o valor de uma única propriedade, ou {@code null} se ausente.
     *
     * @param element      o elemento BPMN a inspecionar
     * @param propertyName nome da propriedade
     * @param adapter      o adapter do engine configurado
     * @return o valor da propriedade, ou {@code null}
     */
    public static String extractOne(BaseElement element, String propertyName,
                                    EngineAdapter adapter) {
        return extract(element, adapter).get(propertyName);
    }
}
