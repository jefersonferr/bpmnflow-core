package org.bpmnflow.parser;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class BpmnPropertiesConfig {

    /**
     * Engine alvo. Determina qual {@link org.bpmnflow.parser.engine.EngineAdapter}
     * será instanciado pelo {@link org.bpmnflow.parser.engine.EngineAdapterFactory}.
     *
     * <p>Valores válidos: {@code "camunda7"} | {@code "camunda8"}.</p>
     * <p>Default: {@code "camunda7"} — garante backward compatibility para
     * configs existentes que não declarem o campo {@code engine}.</p>
     */
    private String engine = "camunda7";

    /**
     * Mapa de propriedades por tipo de elemento BPMN.
     * Inicializado como mapa vazio (nunca null) para garantir que
     * BpmnPropertiesLoader.getPropertiesForType() nunca receba um null
     * ao chamar containsKey(), independente de como esta classe for instanciada.
     */
    private Map<String, List<ModelProperty>> extensionProperties = Collections.emptyMap();
}
