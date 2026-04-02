package org.bpmnflow.model;

/**
 * Typed inconsistency codes. Replaces the magic integers that were
 * scattered across ModelParser. Each constant owns its own code and
 * default message template so call sites never hard-code either.
 *
 * Codes 1-99: field-level validation (missing attribute or property).
 * Codes 100+: structural validation (missing element).
 */
public enum InconsistencyCode {

    // --- Participant / Process ---
    PARTICIPANT_NAME_MISSING        (1,  "Missing attribute 'name' in element %s"),
    PROCESS_ID_MISSING              (2,  "Missing attribute 'Process ID' in element %s"),
    PROCESS_VERSION_MISSING         (3,  "Missing attribute 'Version tag' in element %s"),
    PROCESS_DOCUMENTATION_MISSING   (4,  "Missing attribute 'Process Documentation' in element %s"),
    PROCESS_TYPE_MISSING            (5,  "Missing extension property 'process_type' in element %s"),
    PROCESS_SUBTYPE_MISSING         (6,  "Missing extension property 'process_subtype' in element %s"),

    // --- Lane ---
    LANE_STAGE_MISSING              (7,  "Missing extension property 'stage' in element %s"),
    LANE_NAME_MISSING               (8,  "Missing attribute 'name' in element %s"),

    // --- Task ---
    TASK_NAME_MISSING               (9,  "Missing attribute 'name' in element %s"),
    TASK_STAGE_MISSING              (10, "Missing extension property 'stage' in element %s"),
    TASK_ACTIVITY_MISSING           (11, "Missing extension property 'activity' in element %s"),

    // --- Start / End Event ---
    EVENT_PROCESS_STATUS_MISSING    (12, "Missing extension property 'process_status' in element %s"),

    // --- Sequence Flow ---
    SEQUENCE_FLOW_NAME_MISSING      (13, "Missing attribute 'name' in element %s"),
    SEQUENCE_FLOW_CONCLUSION_MISSING(14, "Missing extension property 'conclusion' in element %s"),

    // --- Structural ---
    PARTICIPANT_REQUIRED            (100, "At least one element 'Participant' is required"),
    DOCUMENTATION_REQUIRED          (101, "At least one nested element 'Documentation' is required in element %s"),
    LANE_REQUIRED                   (102, "At least one element 'Lane' is required"),
    PROCESS_REQUIRED                (103, "Element 'Process' is required in collaboration with element %s");

    private final int code;
    private final String messageTemplate;

    InconsistencyCode(int code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public int getCode() {
        return code;
    }

    /**
     * Creates an {@link Inconsistency} with no extra arguments (structural codes).
     */
    public Inconsistency of() {
        return new Inconsistency(code, messageTemplate);
    }

    /**
     * Creates an {@link Inconsistency} formatting the message with the given element id.
     */
    public Inconsistency of(String elementId) {
        return new Inconsistency(code, messageTemplate.formatted(elementId));
    }
}
