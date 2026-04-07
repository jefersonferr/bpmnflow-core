package org.bpmnflow.parser.engine;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for Camunda 7.
 *
 * <p>Uses the generic DOM API ({@code getDomElement()}) provided by the
 * {@code zeebe-bpmn-model} library to access elements in the
 * {@code http://camunda.org/schema/1.0/bpmn} namespace. This is necessary
 * because the Camunda 7 library ({@code camunda-bpmn-model}) has reached
 * end-of-life and its typed classes ({@code CamundaProperties},
 * {@code CamundaProperty}) do not exist in {@code zeebe-bpmn-model}.</p>
 *
 * <h2>Expected XML structure (C7)</h2>
 * <pre>{@code
 * <bpmn:extensionElements>
 *   <camunda:properties>
 *     <camunda:property name="stage" value="ST" />
 *   </camunda:properties>
 * </bpmn:extensionElements>
 *
 * <bpmn:process camunda:versionTag="1.0" ...>
 * }</pre>
 */
public class Camunda7EngineAdapter implements EngineAdapter {

    private static final String CAMUNDA7_NS = "http://camunda.org/schema/1.0/bpmn";

    @Override
    public Map<String, String> extractProperties(BaseElement element) {
        Map<String, String> attributes = new HashMap<>();
        ExtensionElements extensionElements = element.getExtensionElements();
        if (extensionElements == null) return attributes;

        for (ModelElementInstance instance : extensionElements.getElements()) {
            DomElement domEl = instance.getDomElement();

            if (!CAMUNDA7_NS.equals(domEl.getNamespaceURI())) continue;
            if (!"properties".equals(domEl.getLocalName()))   continue;

            for (DomElement child : domEl.getChildElements()) {
                if (!"property".equals(child.getLocalName())) continue;

                String name  = child.getAttribute(CAMUNDA7_NS, "name");
                String value = child.getAttribute(CAMUNDA7_NS, "value");

                // Fallback: some exporters omit the namespace qualifier on attributes
                if (name  == null) name  = child.getAttribute("", "name");
                if (value == null) value = child.getAttribute("", "value");
                if (name  != null) attributes.put(name, value);
            }
        }
        return attributes;
    }

    @Override
    public String extractVersionTag(BaseElement process) {
        return process.getAttributeValueNs(CAMUNDA7_NS, "versionTag");
    }

    @Override
    public String engineId() {
        return "camunda7";
    }
}