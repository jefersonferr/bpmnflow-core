package org.bpmnflow.model;

import lombok.Getter;

/**
 * Immutable key-value pair that represents a single entry in one of the
 * three collections of {@link ApiHandlerDefinition}:
 * {@code taskHeaders}, {@code inputMappings}, or {@code outputMappings}.
 * Semantics by collection:
 *   taskHeaders   → key = header name,        value = static header value
 *   inputMappings → C7: key = inputParameter name, value = text content
 *                   C8: key = target variable, value = source FEEL expression
 *   outputMappings→ C7: key = outputParameter name, value = text content
 *                   C8: key = target variable, value = source FEEL expression
 */
@Getter
public class ApiField {

    private final String key;
    private final String value;

    public ApiField(String key, String value) {
        this.key   = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ApiField{key='" + key + "', value='" + value + "'}";
    }
}