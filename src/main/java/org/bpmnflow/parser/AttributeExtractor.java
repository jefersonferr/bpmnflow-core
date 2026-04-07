package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import org.bpmnflow.parser.engine.EngineAdapter;

import java.util.Map;

/**
 * Convenience facade for extracting extension properties from BPMN elements.
 *
 * <p>All extraction logic has been moved to the {@link EngineAdapter} implementations.
 * This class is a static access point that handlers use to keep their code concise.</p>
 */
public final class AttributeExtractor {

    private AttributeExtractor() {}

    /**
     * Extracts all extension properties from the element, delegating to the active adapter.
     *
     * @param element the BPMN element to inspect
     * @param adapter the configured engine adapter
     * @return name→value map; never null
     */
    public static Map<String, String> extract(BaseElement element, EngineAdapter adapter) {
        return adapter.extractProperties(element);
    }

    /**
     * Returns the value of a single property, or {@code null} if absent.
     *
     * @param element      the BPMN element to inspect
     * @param propertyName the property name
     * @param adapter      the configured engine adapter
     * @return the property value, or {@code null}
     */
    public static String extractOne(BaseElement element, String propertyName,
                                    EngineAdapter adapter) {
        return extract(element, adapter).get(propertyName);
    }
}
