package org.bpmnflow.parser.engine;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.ExtensionElements;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeHeader;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeInput;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeIoMapping;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeOutput;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeProperties;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskHeaders;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeVersionTag;
import org.bpmnflow.model.ApiField;
import org.bpmnflow.model.ApiHandlerDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for Camunda 8 / Zeebe.
 *
 * <p>Uses the native typed API from {@code zeebe-bpmn-model} to access
 * elements in the {@code http://camunda.org/schema/zeebe/1.0} namespace.</p>
 *
 * <h2>Expected XML structure — extension properties (C8)</h2>
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
 *
 * <h2>Expected XML structure — API connector (C8)</h2>
 * <pre>{@code
 * <bpmn:extensionElements>
 *   <zeebe:taskDefinition type="payment-authorize" retries="3" />
 *   <zeebe:taskHeaders>
 *     <zeebe:header key="endpoint" value="https://api.example.com/v1/authorize" />
 *     <zeebe:header key="method"   value="POST" />
 *   </zeebe:taskHeaders>
 *   <zeebe:ioMapping>
 *     <zeebe:input  source="=cliente_id"  target="cliente_id" />
 *     <zeebe:output source="=txn_id"      target="pagamento_txn_id" />
 *   </zeebe:ioMapping>
 * </bpmn:extensionElements>
 * }</pre>
 */
public class Camunda8EngineAdapter implements EngineAdapter {

    private static final Set<String> RESERVED_HEADER_KEYS = Set.of("endpoint", "method");

    // ---------------------------------------------------------------
    // extractProperties
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // extractVersionTag
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // extractApiHandler
    // ---------------------------------------------------------------

    /**
     * Reads {@code zeebe:taskDefinition}, {@code zeebe:taskHeaders}, and
     * {@code zeebe:ioMapping}.
     *
     * <p>Returns {@code null} when no {@code zeebe:taskDefinition} element is
     * found so that {@link org.bpmnflow.parser.FlowNodeHandler} can treat this
     * as a plain {@link org.bpmnflow.model.ActivityNode}.</p>
     */
    @Override
    public ApiHandlerDefinition extractApiHandler(BaseElement element) {
        ExtensionElements extensionElements = element.getExtensionElements();
        if (extensionElements == null) return null;

        ZeebeTaskDefinition taskDef = extensionElements.getElements().stream()
                .filter(e -> e instanceof ZeebeTaskDefinition)
                .map(e -> (ZeebeTaskDefinition) e)
                .findFirst()
                .orElse(null);

        if (taskDef == null) return null;

        String connectorId = taskDef.getType();
        int    retries     = parseRetries(taskDef.getRetries());

        String         endpoint    = null;
        String         method      = null;
        List<ApiField> taskHeaders = new ArrayList<>();

        ZeebeTaskHeaders headersEl = extensionElements.getElements().stream()
                .filter(e -> e instanceof ZeebeTaskHeaders)
                .map(e -> (ZeebeTaskHeaders) e)
                .findFirst()
                .orElse(null);

        if (headersEl != null) {
            for (ZeebeHeader header : headersEl.getHeaders()) {
                String key   = header.getKey();
                String value = header.getValue();
                if ("endpoint".equals(key)) {
                    endpoint = value;
                } else if ("method".equals(key)) {
                    method = value;
                } else if (!RESERVED_HEADER_KEYS.contains(key)) {
                    taskHeaders.add(new ApiField(key, value));
                }
            }
        }

        List<ApiField> inputMappings  = new ArrayList<>();
        List<ApiField> outputMappings = new ArrayList<>();

        ZeebeIoMapping ioMapping = extensionElements.getElements().stream()
                .filter(e -> e instanceof ZeebeIoMapping)
                .map(e -> (ZeebeIoMapping) e)
                .findFirst()
                .orElse(null);

        if (ioMapping != null) {
            for (ZeebeInput input : ioMapping.getInputs()) {
                inputMappings.add(new ApiField(input.getTarget(), input.getSource()));
            }
            for (ZeebeOutput output : ioMapping.getOutputs()) {
                outputMappings.add(new ApiField(output.getTarget(), output.getSource()));
            }
        }

        return ApiHandlerDefinition.builder()
                .connectorId(connectorId)
                .endpoint(endpoint)
                .method(method)
                .retries(retries)
                .taskHeaders(taskHeaders)
                .inputMappings(inputMappings)
                .outputMappings(outputMappings)
                .build();
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Parses the retries string from {@code zeebe:taskDefinition}.
     * Returns {@code 0} when the attribute is absent, blank, or non-numeric.
     */
    private static int parseRetries(String retries) {
        if (retries == null || retries.isBlank()) return 0;
        try {
            return Integer.parseInt(retries.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String engineId() {
        return "camunda8";
    }
}