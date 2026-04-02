package org.bpmnflow.parser;

/**
 * Thrown when the BPMN properties configuration cannot be loaded or is
 * structurally invalid.
 *
 * <p>This is an unchecked exception — callers that cannot recover from a
 * missing or broken config (the majority) are not forced to catch it, while
 * callers that need to handle it gracefully (e.g. a Spring Boot auto-configure
 * that falls back to defaults) can still do so explicitly.</p>
 */
public class BpmnConfigException extends RuntimeException {

    public BpmnConfigException(String message) {
        super(message);
    }

    public BpmnConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}