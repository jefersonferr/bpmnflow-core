package org.bpmnflow.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.StringJoiner;

/**
 * Holds all API-call metadata extracted from a {@code ServiceTask}'s
 * engine-specific connector declaration.
 *
 * <ul>
 *   <li>Camunda 7 — read from {@code &lt;camunda:connector&gt;}:
 *       {@code connectorId}, {@code inputParameters}, {@code outputParameters}.</li>
 *   <li>Camunda 8 — read from {@code &lt;zeebe:taskDefinition&gt;} +
 *       {@code &lt;zeebe:taskHeaders&gt;} + {@code &lt;zeebe:ioMapping&gt;}.</li>
 * </ul>
 *
 * <p>Built by {@link org.bpmnflow.parser.engine.EngineAdapter#extractApiHandler}
 * and stored in {@link ApiActivityNode}. Consumers access it via
 * {@code ApiActivityNode.getApiHandler()}.</p>
 *
 * <p>All collections are never null — empty lists when the corresponding
 * XML sections are absent.</p>
 */
@Getter
@Builder
public class ApiHandlerDefinition {

    /**
     * Connector / job type identifier.
     * <ul>
     *   <li>C7: value of {@code &lt;camunda:connectorId&gt;}</li>
     *   <li>C8: {@code type} attribute of {@code &lt;zeebe:taskDefinition&gt;}</li>
     * </ul>
     */
    private final String connectorId;

    /**
     * HTTP endpoint URL.
     * <ul>
     *   <li>C7: {@code &lt;camunda:inputParameter name="url"&gt;}</li>
     *   <li>C8: {@code &lt;zeebe:taskHeader key="endpoint"&gt;}</li>
     * </ul>
     */
    private final String endpoint;

    /**
     * HTTP method (GET, POST, PUT, PATCH, DELETE).
     * <ul>
     *   <li>C7: {@code &lt;camunda:inputParameter name="method"&gt;}</li>
     *   <li>C8: {@code &lt;zeebe:taskHeader key="method"&gt;}</li>
     * </ul>
     */
    private final String method;

    /**
     * Number of automatic retries on failure.
     * <ul>
     *   <li>C7: not natively declared on the element; defaults to 0.</li>
     *   <li>C8: {@code retries} attribute on {@code &lt;zeebe:taskDefinition&gt;}.</li>
     * </ul>
     */
    private final int retries;

    /** Static headers passed to the connector/worker at runtime. */
    private final List<ApiField> taskHeaders;

    /** Process-variable-to-job input mappings. */
    private final List<ApiField> inputMappings;

    /** Job-output-to-process-variable mappings. */
    private final List<ApiField> outputMappings;

    @Override
    public String toString() {
        return "ApiHandlerDefinition{" +
                "connectorId='" + connectorId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", method='" + method + '\'' +
                ", retries=" + retries +
                ", taskHeaders=" + format(taskHeaders) +
                ", inputMappings=" + format(inputMappings) +
                ", outputMappings=" + format(outputMappings) +
                '}';
    }

    private static String format(List<ApiField> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        list.forEach(f -> sj.add(f.toString()));
        return sj.toString();
    }
}