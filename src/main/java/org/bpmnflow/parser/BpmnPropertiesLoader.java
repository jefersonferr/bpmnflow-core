package org.bpmnflow.parser;

import java.util.logging.Logger;

public class BpmnPropertiesLoader {

    private static final Logger LOGGER = Logger.getLogger(BpmnPropertiesLoader.class.getName());

    private final BpmnPropertiesConfig config;

    public BpmnPropertiesLoader(BpmnPropertiesConfig config) {
        this.config = config;
    }

    public ModelProperty getParticipant(String propertyName) {
        return getPropertiesForType("participant", propertyName);
    }

    public ModelProperty getProcess(String propertyName) {
        return getPropertiesForType("process", propertyName);
    }

    public ModelProperty getLane(String propertyName) {
        return getPropertiesForType("lane", propertyName);
    }

    public ModelProperty getTask(String propertyName) {
        return getPropertiesForType("task", propertyName);
    }

    public ModelProperty getStartEvent(String propertyName) {
        return getPropertiesForType("startEvent", propertyName);
    }

    public ModelProperty getEndEvent(String propertyName) {
        return getPropertiesForType("endEvent", propertyName);
    }

    public ModelProperty getSequenceFlow(String propertyName) {
        return getPropertiesForType("sequenceFlow", propertyName);
    }

    /**
     * Busca uma propriedade pelo tipo de elemento e nome.
     *
     * <p>Nunca retorna null. Quando o tipo de elemento não está mapeado no config,
     * ou quando a propriedade nomeada não existe dentro do tipo, retorna
     * {@link ModelProperty#ABSENT} — uma sentinela com required=false e
     * extension=false — permitindo que os call sites façam .isRequired()
     * diretamente sem nenhum null-check.</p>
     *
     * <p>Níveis de log:
     * <ul>
     *   <li>WARNING — tipo de elemento inteiramente ausente do config: provável erro de configuração.</li>
     *   <li>FINE    — propriedade ausente dentro de um tipo mapeado: uso intencional e válido.</li>
     * </ul>
     * </p>
     *
     * @param elementType  chave do elemento BPMN (ex: "task", "lane")
     * @param propertyName nome da propriedade (ex: "stage", "activity")
     * @return a {@link ModelProperty} configurada, ou {@link ModelProperty#ABSENT}
     */
    private ModelProperty getPropertiesForType(String elementType, String propertyName) {
        if (config.getExtensionProperties() == null || !config.getExtensionProperties().containsKey(elementType)) {
            LOGGER.warning(String.format(
                    "Element type '%s' not found in config — property '%s' treated as not required.",
                    elementType, propertyName
            ));
            return ModelProperty.ABSENT;
        }

        return config.getExtensionProperties()
                .get(elementType)
                .stream()
                .filter(p -> propertyName.equals(p.getName()))
                .findFirst()
                .orElseGet(() -> {
                    LOGGER.fine(String.format(
                            "Property '%s' not defined for element type '%s' — treated as not required.",
                            propertyName, elementType
                    ));
                    return ModelProperty.ABSENT;
                });
    }
}
