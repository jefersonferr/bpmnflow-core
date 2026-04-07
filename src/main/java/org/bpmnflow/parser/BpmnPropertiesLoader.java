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
     * Looks up a property by element type and name.
     *
     * <p>Never returns null. When the element type is not mapped in the config,
     * or when the named property does not exist within the type, returns
     * {@link ModelProperty#ABSENT} — a sentinel with required=false and
     * extension=false — allowing call sites to call .isRequired() directly
     * without any null-check.</p>
     *
     * <p>Log levels:
     * <ul>
     *   <li>WARNING — element type entirely absent from config: likely a configuration error.</li>
     *   <li>FINE    — property absent within a mapped type: intentional and valid usage.</li>
     * </ul>
     * </p>
     *
     * @param elementType  the BPMN element key (e.g. "task", "lane")
     * @param propertyName the property name (e.g. "stage", "activity")
     * @return the configured {@link ModelProperty}, or {@link ModelProperty#ABSENT}
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
