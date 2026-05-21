package org.bpmnflow.parser.engine;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import org.bpmnflow.model.ApiHandlerDefinition;

import java.util.Map;

/**
 * SPI (Service Provider Interface) that abstracts access to engine-specific
 * elements within a BPMN file.
 *
 * <p>Each engine (Camunda 7, Camunda 8) stores extension properties and the
 * version tag in different XML namespaces and structures. This interface
 * isolates those differences from the rest of the framework, which remains
 * engine-agnostic.</p>
 *
 * <p>Implementations are created by {@link EngineAdapterFactory} based on the
 * {@code engine} field in {@code config.yaml}. No code outside this package
 * should depend on a concrete implementation — always use the {@link EngineAdapter}
 * type.</p>
 *
 * <h2>Namespace conventions</h2>
 * <ul>
 *   <li>Camunda 7: {@code http://camunda.org/schema/1.0/bpmn} (prefix {@code camunda:})</li>
 *   <li>Camunda 8: {@code http://camunda.org/schema/zeebe/1.0} (prefix {@code zeebe:})</li>
 * </ul>
 */
public interface EngineAdapter {

    /**
     * Extracts all extension properties from the given BPMN element.
     *
     * @param element any BPMN element that may have extensionElements
     * @return mutable name→value map; never null, may be empty
     */
    Map<String, String> extractProperties(BaseElement element);

    /**
     * Extracts the version tag from the {@code &lt;process&gt;} element.
     *
     * <ul>
     *   <li>C7: namespace-qualified attribute {@code camunda:versionTag} on the
     *       {@code &lt;process&gt;} element itself</li>
     *   <li>C8: child element {@code &lt;zeebe:versionTag value="..."&gt;}
     *       inside extensionElements</li>
     * </ul>
     *
     * @param process the Process element of the BPMN model
     * @return the version tag value, or {@code null} if absent
     */
    String extractVersionTag(BaseElement process);

    /**
     * Extracts the API handler definition from a {@code ServiceTask} element.
     *
     * <p>Each engine declares API call metadata in a completely different XML
     * structure:</p>
     * <ul>
     *   <li>Camunda 7: reads {@code &lt;camunda:connector&gt;} —
     *       {@code connectorId}, {@code inputParameter} entries (url, method,
     *       payload, extra headers), and {@code outputParameter} entries.</li>
     *   <li>Camunda 8: reads {@code &lt;zeebe:taskDefinition&gt;} for {@code type}
     *       and {@code retries}; {@code &lt;zeebe:taskHeaders&gt;} for static
     *       headers (endpoint, method, extras); {@code &lt;zeebe:ioMapping&gt;}
     *       for input/output variable mappings.</li>
     * </ul>
     *
     * @param element a BPMN element — typically a {@code ServiceTask}
     * @return a populated {@link ApiHandlerDefinition}, or {@code null} if
     *         the element carries no connector / taskDefinition declaration
     */
    ApiHandlerDefinition extractApiHandler(BaseElement element);

    /**
     * Engine identifier. Values: {@code "camunda7"}, {@code "camunda8"}.
     */
    String engineId();
}