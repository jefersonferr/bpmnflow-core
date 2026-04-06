package org.bpmnflow.parser.engine;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.ExtensionElements;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeProperties;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeVersionTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter para Camunda 8 / Zeebe.
 *
 * <p>Usa a API tipada nativa do {@code zeebe-bpmn-model} para acessar
 * elementos no namespace {@code http://camunda.org/schema/zeebe/1.0}.</p>
 *
 * <h2>Estrutura XML esperada (C8)</h2>
 * <pre>{@code
 * <bpmn:extensionElements>
 *   <zeebe:properties>
 *     <zeebe:property name="stage" value="ST" />
 *   </zeebe:properties>
 * </bpmn:extensionElements>
 *
 * <bpmn:process id="...">
 *   <bpmn:extensionElements>
 *     <zeebe:versionTag value="1.0" />
 *   </bpmn:extensionElements>
 * </bpmn:process>
 * }</pre>
 */
public class Camunda8EngineAdapter implements EngineAdapter {

    @Override
    public Map<String, String> extractProperties(BaseElement element) {
        Map<String, String> attributes = new HashMap<>();
        ExtensionElements extensionElements = element.getExtensionElements();
        if (extensionElements == null) return attributes;

        extensionElements.getElements().stream()
                .filter(e -> e instanceof ZeebeProperties)
                .map(e -> (ZeebeProperties) e)
                .forEach(props -> props.getProperties()
                        .forEach(p -> attributes.put(p.getName(), p.getValue())));

        return attributes;
    }

    @Override
    public String extractVersionTag(BaseElement process) {
        ExtensionElements extensionElements = process.getExtensionElements();
        if (extensionElements == null) return null;

        return extensionElements.getElements().stream()
                .filter(e -> e instanceof ZeebeVersionTag)
                .map(e -> (ZeebeVersionTag) e)
                .findFirst()
                .map(ZeebeVersionTag::getValue)
                .orElse(null);
    }

    @Override
    public String engineId() {
        return "camunda8";
    }
}
