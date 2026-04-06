package org.bpmnflow.parser;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

/**
 * Utility for extracting Camunda extension properties from BPMN elements.
 *
 * <p>Previously a private static method on {@code ModelParser}. Extracted to
 * allow independent unit testing and reuse across all {@link ElementHandler}
 * implementations without circular dependencies.</p>
 */
public final class AttributeExtractor {

    private AttributeExtractor() {}

    /**
     * Returns a map of all {@code <camunda:property>} entries defined in the
     * extension elements of {@code element}, keyed by property name.
     *
     * @param element the BPMN element to inspect
     * @return a mutable map of extension property name → value; never null
     */
    public static Map<String, String> extract(BaseElement element) {
        Map<String, String> attributes = new HashMap<>();
        ExtensionElements extensionElements = element.getExtensionElements();
        if (nonNull(extensionElements)) {
            for (ModelElementInstance instance : extensionElements.getElements()) {
                if (instance instanceof CamundaProperties camundaProperties) {
                    for (CamundaProperty property : camundaProperties.getCamundaProperties()) {
                        attributes.put(property.getCamundaName(), property.getCamundaValue());
                    }
                }
            }
        }
        return attributes;
    }

    /**
     * Convenience overload: returns a single property value or {@code null}.
     */
    public static String extractOne(BaseElement element, String propertyName) {
        return extract(element).get(propertyName);
    }
}
