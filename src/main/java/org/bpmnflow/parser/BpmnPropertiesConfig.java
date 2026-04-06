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
     * Mapa de propriedades por tipo de elemento BPMN.
     * Inicializado como mapa vazio (nunca null) para garantir que
     * BpmnPropertiesLoader.getPropertiesForType() nunca receba um null
     * ao chamar containsKey(), independente de como esta classe for instanciada.
     */
    private Map<String, List<ModelProperty>> extensionProperties = Collections.emptyMap();
}