package org.bpmnflow.parser;

/**
 * Thrown when the BPMN model itself cannot be read — the stream is missing,
 * is not well-formed XML, or does not conform to the BPMN 2.0 schema.
 *
 * <p>Unchecked, like {@link BpmnConfigException}: it extends
 * {@link RuntimeException}, so callers that already catch
 * {@code RuntimeException} keep working, while callers that want to tell a
 * broken model apart from a broken configuration can catch it explicitly.</p>
 */
public class BpmnModelException extends RuntimeException {

    public BpmnModelException(String message) {
        super(message);
    }

    public BpmnModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
