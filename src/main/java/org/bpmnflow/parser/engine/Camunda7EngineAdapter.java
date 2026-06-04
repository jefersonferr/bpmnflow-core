package org.bpmnflow.parser.engine;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.ExtensionElements;
import org.bpmnflow.model.ApiField;
import org.bpmnflow.model.ApiHandlerDefinition;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <h2>Expected XML structure — extension properties (C7)</h2>
 * <pre>{@code
 * <bpmn:extensionElements>
 *   <camunda:properties>
 *     <camunda:property name="stage" value="ST" />
 *   </camunda:properties>
 * </bpmn:extensionElements>
 *
 * <bpmn:process camunda:versionTag="1.0" ...>
 * }</pre>
 *
 * <h2>Expected XML structure — API connector (C7)</h2>
 * <pre>{@code
 * <bpmn:extensionElements>
 *   <camunda:connector>
 *     <camunda:connectorId>http-connector</camunda:connectorId>
 *     <camunda:inputOutput>
 *       <camunda:inputParameter name="url">https://api.example.com</camunda:inputParameter>
 *       <camunda:inputParameter name="method">POST</camunda:inputParameter>
 *       <camunda:outputParameter name="txn_id">...</camunda:outputParameter>
 *     </camunda:inputOutput>
 *   </camunda:connector>
 * </bpmn:extensionElements>
 * }</pre>
 */
public class Camunda7EngineAdapter implements EngineAdapter {

    private static final String CAMUNDA7_NS = "http://camunda.org/schema/1.0/bpmn";

    private static final Set<String> RESERVED_INPUT_PARAMS =
            Set.of("url", "method", "payload", "headers");

    // ---------------------------------------------------------------
    // extractProperties
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // extractVersionTag
    // ---------------------------------------------------------------

    @Override
    public String extractVersionTag(BaseElement process) {
        return process.getAttributeValueNs(CAMUNDA7_NS, "versionTag");
    }

    // ---------------------------------------------------------------
    // extractApiHandler
    // ---------------------------------------------------------------

    /**
     * Reads the {@code camunda:connector} block inside extensionElements.
     *
     * <p>Returns {@code null} when no {@code camunda:connector} element is found
     * so that {@link org.bpmnflow.parser.FlowNodeHandler} can treat this as a
     * plain {@link org.bpmnflow.model.ActivityNode}.</p>
     */
    @Override
    public ApiHandlerDefinition extractApiHandler(BaseElement element) {
        ExtensionElements extensionElements = element.getExtensionElements();
        if (extensionElements == null) return null;

        for (ModelElementInstance instance : extensionElements.getElements()) {
            DomElement domEl = instance.getDomElement();
            if (!CAMUNDA7_NS.equals(domEl.getNamespaceURI())) continue;
            if ("connector".equals(domEl.getLocalName()))
                return parseConnector(domEl);
        }
        return null;
    }

    private ApiHandlerDefinition parseConnector(DomElement connector) {
        String connectorId = null;
        String url         = null;
        String method      = null;
        List<ApiField> taskHeaders    = new ArrayList<>();
        List<ApiField> inputMappings  = new ArrayList<>();
        List<ApiField> outputMappings = new ArrayList<>();

        for (DomElement child : connector.getChildElements()) {
            String localName = child.getLocalName();

            if ("connectorId".equals(localName)) {
                connectorId = child.getTextContent();
                continue;
            }

            if ("inputOutput".equals(localName)) {
                for (DomElement ioChild : child.getChildElements()) {
                    String ioLocal = ioChild.getLocalName();

                    if ("inputParameter".equals(ioLocal)) {
                        String name  = attr(ioChild, "name");
                        String value = ioChild.getTextContent();

                        if ("url".equals(name)) {
                            url = value;
                        } else if ("method".equals(name)) {
                            method = value;
                        } else if (!RESERVED_INPUT_PARAMS.contains(name) && name != null) {
                            taskHeaders.add(new ApiField(name, value));
                            inputMappings.add(new ApiField(name, value));
                        } else if (name != null) {
                            inputMappings.add(new ApiField(name, value));
                        }
                    }

                    if ("outputParameter".equals(ioLocal)) {
                        String name  = attr(ioChild, "name");
                        String value = ioChild.getTextContent();
                        if (name != null) {
                            outputMappings.add(new ApiField(name, value));
                        }
                    }
                }
            }
        }

        if (connectorId == null) return null;

        return ApiHandlerDefinition.builder()
                .connectorId(connectorId)
                .endpoint(url)
                .method(method)
                .retries(0)
                .taskHeaders(taskHeaders)
                .inputMappings(inputMappings)
                .outputMappings(outputMappings)
                .build();
    }

    /** Reads an attribute without namespace qualification (C7 exporters omit ns on attrs). */
    private static String attr(DomElement el, String name) {
        String v = el.getAttribute(CAMUNDA7_NS, name);
        return v != null ? v : el.getAttribute("", name);
    }

    @Override
    public String engineId() {
        return "camunda7";
    }
}