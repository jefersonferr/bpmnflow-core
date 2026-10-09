package org.bpmnflow.model;

/**
 * Semantic type of a workflow rule extracted from the BPMN model.
 * Each value represents a connectivity pattern between diagram elements.
 *
 * <p>Each type also declares whether it is an <em>initial</em> rule (the
 * process starts here, so the rule has no source activity) or a
 * <em>final</em> rule (the process ends here, so the rule has no target
 * activity). {@link WorkflowRule} enforces these invariants.</p>
 */
public enum RuleType {

    /** StartEvent → Task: flow entry rule. Initial — source is always {@code null}. */
    START_TO_TASK(1, true, false),

    /** Task → Task: direct transition between activities. */
    TASK_TO_TASK(2, false, false),

    /** Task → Merge → EndEvent: process end via merge gateway. Final — target is always {@code null}. */
    TASK_TO_MERGE_TO_END(3, false, true),

    /** Task → Merge → Task: flow continuation via merge gateway. */
    TASK_TO_MERGE_TO_TASK(4, false, false),

    /** Split → Task: flow branching via exclusive gateway. */
    SPLIT_TO_TASK(5, false, false),

    /** Split → Merge: direct connection between split and merge gateways. */
    SPLIT_TO_MERGE(6, false, false),

    /** Task → EndEvent: direct process end. Final — target is always {@code null}. */
    TASK_TO_END(7, false, true),

    /** Task → Split → EndEvent: process end via split gateway. Final — target is always {@code null}. */
    TASK_TO_SPLIT_TO_END(8, false, true);

    private final int code;
    private final boolean initial;
    private final boolean terminal;

    RuleType(int code, boolean initial, boolean terminal) {
        this.code = code;
        this.initial = initial;
        this.terminal = terminal;
    }

    public int getCode() {
        return code;
    }

    /**
     * {@code true} when this rule starts the process — its source is
     * the StartEvent and therefore {@code null} in {@link WorkflowRule}.
     */
    public boolean isInitial() {
        return initial;
    }

    /**
     * {@code true} when this rule ends the process — its target is
     * the EndEvent and therefore {@code null} in {@link WorkflowRule}.
     */
    public boolean isFinal() {
        return terminal;
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
