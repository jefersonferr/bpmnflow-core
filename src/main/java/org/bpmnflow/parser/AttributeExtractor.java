package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.Documentation;
import org.bpmnflow.parser.engine.EngineAdapter;

import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Returns the text of the element's {@code <bpmn:documentation>} children.
     *
     * <p>In BPMN 2.0 documentation is a child element, not an attribute —
     * {@code getAttributeValue("documentation")} always returns {@code null}.
     * Multiple documentation entries are joined with a line break; blank
     * entries are ignored.</p>
     *
     * @param element the BPMN element to inspect
     * @return the documentation text, or {@code null} when absent or blank
     */
    public static String documentation(BaseElement element) {
        String text = element.getDocumentations().stream()
                .map(Documentation::getTextContent)
                .filter(t -> t != null && !t.isBlank())
                .map(String::strip)
                .collect(Collectors.joining("\n"));
        return text.isEmpty() ? null : text;
    }
}
