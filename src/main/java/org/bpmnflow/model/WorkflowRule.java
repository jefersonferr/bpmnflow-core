package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * A transition rule derived from the BPMN topology.
 *
 * <h2>Endpoint invariants</h2>
 * <ul>
 *   <li>Only <em>initial</em> rules ({@link RuleType#isInitial()}) have a
 *       {@code null} source — the process starts at the StartEvent.</li>
 *   <li>Only <em>final</em> rules ({@link RuleType#isFinal()}) have a
 *       {@code null} target — the process ends at the EndEvent.</li>
 * </ul>
 * <p>These invariants are enforced by the constructor and by
 * {@link #setType}, {@link #setSource} and {@link #setTarget}; a violation
 * throws {@link IllegalArgumentException}.</p>
 */
@Getter
public class WorkflowRule {

    RuleType type;
    ActivityNode source;
    ActivityNode target;
    @Setter Conclusion conclusion;
    @Setter String processStatus;

    public WorkflowRule(RuleType type, ActivityNode source, ActivityNode target,
                        Conclusion conclusion, String processStatus) {
        Objects.requireNonNull(type, "type must not be null");
        checkEndpoints(type, source, target);
        this.type = type;
        this.source = source;
        this.target = target;
        this.conclusion = conclusion;
        this.processStatus = processStatus;
    }

    /** {@code true} when this rule starts the process (source is {@code null}). */
    public boolean isInitial() {
        return type.isInitial();
    }

    /** {@code true} when this rule ends the process (target is {@code null}). */
    public boolean isFinal() {
        return type.isFinal();
    }

    public void setType(RuleType type) {
        Objects.requireNonNull(type, "type must not be null");
        checkEndpoints(type, source, target);
        this.type = type;
    }

    public void setSource(ActivityNode source) {
        checkEndpoints(type, source, target);
        this.source = source;
    }

    public void setTarget(ActivityNode target) {
        checkEndpoints(type, source, target);
        this.target = target;
    }

    /**
     * Checks the endpoint invariants without throwing.
     *
     * @return {@code true} when the source is {@code null} if and only if the
     *         type is initial, and the target is {@code null} if and only if
     *         the type is final
     */
    public static boolean endpointsValid(RuleType type, ActivityNode source, ActivityNode target) {
        return type != null
                && type.isInitial() == (source == null)
                && type.isFinal() == (target == null);
    }

    private static void checkEndpoints(RuleType type, ActivityNode source, ActivityNode target) {
        if (!endpointsValid(type, source, target)) {
            throw new IllegalArgumentException(
                    "Invalid endpoints for " + type
                            + ": source " + (source == null ? "null" : source.getAbbreviation())
                            + ", target " + (target == null ? "null" : target.getAbbreviation())
                            + " (only initial rules may have a null source"
                            + " and only final rules may have a null target)");
        }
    }

    @Override
    public String toString() {
        return "WorkflowRule{" +
                "type='" + type + '\'' +
                ", source='" + ((source == null) ? "null" : source.getAbbreviation()) + '\'' +
                ", target='" + ((target == null) ? "null" : target.getAbbreviation()) + '\'' +
                ", conclusion='" + ((conclusion == null) ? "null" : conclusion.getCode()) + '\'' +
                ", processStatus='" + processStatus + '\'' +
                '}';
    }
}
