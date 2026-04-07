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
     * Target engine. Determines which {@link org.bpmnflow.parser.engine.EngineAdapter}
     * will be instantiated by {@link org.bpmnflow.parser.engine.EngineAdapterFactory}.
     *
     * <p>Valid values: {@code "camunda7"} | {@code "camunda8"}.</p>
     * <p>Default: {@code "camunda7"} — ensures backward compatibility for
     * existing configs that do not declare the {@code engine} field.</p>
     */
    private String engine = "camunda7";

    /**
     * Map of properties per BPMN element type.
     * Initialised as an empty map (never null) to ensure that
     * BpmnPropertiesLoader.getPropertiesForType() never receives a null when
     * calling containsKey(), regardless of how this class is instantiated.
     */
    private Map<String, List<ModelProperty>> extensionProperties = Collections.emptyMap();
}
