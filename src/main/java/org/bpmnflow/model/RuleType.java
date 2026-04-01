package org.bpmnflow.model;

import lombok.Getter;

/**
 * Semantic type of a workflow rule extracted from the BPMN model.
 * Each value represents a connectivity pattern between diagram elements.
 */
@Getter
public enum RuleType {

    /** StartEvent → Task: flow entry rule. */
    START_TO_TASK(1),

    /** Task → Task: direct transition between activities. */
    TASK_TO_TASK(2),

    /** Task → Merge → EndEvent: process end via merge gateway. */
    TASK_TO_MERGE_TO_END(3),

    /** Task → Merge → Task: flow continuation via merge gateway. */
    TASK_TO_MERGE_TO_TASK(4),

    /** Split → Task: flow branching via exclusive gateway. */
    SPLIT_TO_TASK(5),

    /** Split → Merge: direct connection between split and merge gateways. */
    SPLIT_TO_MERGE(6),

    /** Task → EndEvent: direct process end. */
    TASK_TO_END(7),

    /** Task → Split → EndEvent: process end via split gateway. */
    TASK_TO_SPLIT_TO_END(8);

    private final int code;

    RuleType(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}