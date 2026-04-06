package org.bpmnflow.parser.engine;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.ExtensionElements;
import io.camunda.zeebe.model.xml.instance.DomElement;
import io.camunda.zeebe.model.xml.instance.ModelElementInstance;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter para Camunda 7.
 *
 * <p>Usa a API genérica de DOM ({@code getDomElement()}) da biblioteca
 * {@code zeebe-bpmn-model} para acessar elementos no namespace
 * {@code http://camunda.org/schema/1.0/bpmn}. Isso é necessário porque
 * a biblioteca Camunda 7 ({@code camunda-bpmn-model}) foi descontinuada
 * e as classes tipadas C7 ({@code CamundaProperties}, {@code CamundaProperty})
 * não existem no {@code zeebe-bpmn-model}.</p>
 *
 * <h2>Estrutura XML esperada (C7)</h2>
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

                // Fallback: alguns exportadores omitem o namespace no atributo
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
