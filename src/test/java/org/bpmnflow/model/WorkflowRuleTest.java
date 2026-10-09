package org.bpmnflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkflowRule: endpoint invariants")
class WorkflowRuleTest {

    private final ActivityNode a = new ActivityNode("ST", "A", "Task A", null);
    private final ActivityNode b = new ActivityNode("ST", "B", "Task B", null);

    /** Builds endpoints that are valid for the given type. */
    private WorkflowRule validRule(RuleType type) {
        return new WorkflowRule(type,
                type.isInitial() ? null : a,
                type.isFinal()   ? null : b,
                null, "STATUS");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(RuleType.class)
    @DisplayName("Every RuleType accepts its valid endpoints")
    void validEndpointsAccepted(RuleType type) {
        WorkflowRule rule = assertDoesNotThrow(() -> validRule(type));
        assertEquals(type.isInitial(), rule.isInitial());
        assertEquals(type.isFinal(), rule.isFinal());
        assertEquals(type.isInitial(), rule.getSource() == null);
        assertEquals(type.isFinal(), rule.getTarget() == null);
    }

    @Test
    @DisplayName("Initial and final flags per RuleType")
    void flags() {
        assertTrue(RuleType.START_TO_TASK.isInitial());
        assertFalse(RuleType.START_TO_TASK.isFinal());

        assertTrue(RuleType.TASK_TO_END.isFinal());
        assertTrue(RuleType.TASK_TO_MERGE_TO_END.isFinal());
        assertTrue(RuleType.TASK_TO_SPLIT_TO_END.isFinal());

        for (RuleType t : new RuleType[]{RuleType.TASK_TO_TASK, RuleType.TASK_TO_MERGE_TO_TASK,
                RuleType.SPLIT_TO_TASK, RuleType.SPLIT_TO_MERGE}) {
            assertFalse(t.isInitial(), t::name);
            assertFalse(t.isFinal(), t::name);
        }
    }

    @Test
    @DisplayName("Constructor rejects null source on a non-initial rule")
    void rejectsNullSource() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowRule(RuleType.TASK_TO_TASK, null, b, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowRule(RuleType.TASK_TO_END, null, null, null, "END"));
    }

    @Test
    @DisplayName("Constructor rejects null target on a non-final rule")
    void rejectsNullTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowRule(RuleType.TASK_TO_TASK, a, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowRule(RuleType.START_TO_TASK, null, null, null, "NEW"));
    }

    @Test
    @DisplayName("Constructor rejects a source on an initial rule and a target on a final rule")
    void rejectsUnexpectedEndpoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowRule(RuleType.START_TO_TASK, a, b, null, "NEW"));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowRule(RuleType.TASK_TO_END, a, b, null, "END"));
    }

    @Test
    @DisplayName("Constructor rejects a null type")
    void rejectsNullType() {
        assertThrows(NullPointerException.class,
                () -> new WorkflowRule(null, a, b, null, null));
    }

    @Test
    @DisplayName("Setters enforce the same invariants")
    void settersValidate() {
        WorkflowRule rule = new WorkflowRule(RuleType.TASK_TO_TASK, a, b, null, null);

        assertThrows(IllegalArgumentException.class, () -> rule.setSource(null));
        assertThrows(IllegalArgumentException.class, () -> rule.setTarget(null));
        assertThrows(IllegalArgumentException.class, () -> rule.setType(RuleType.TASK_TO_END));
        assertThrows(NullPointerException.class, () -> rule.setType(null));

        // State is unchanged after rejected updates
        assertSame(a, rule.getSource());
        assertSame(b, rule.getTarget());
        assertEquals(RuleType.TASK_TO_TASK, rule.getType());

        // Valid updates are accepted
        rule.setSource(b);
        rule.setTarget(a);
        assertSame(b, rule.getSource());
        assertSame(a, rule.getTarget());

        rule.setConclusion(new Conclusion("01", "Approved"));
        rule.setProcessStatus("IN_PROGRESS");
        assertEquals("01", rule.getConclusion().getCode());
        assertEquals("IN_PROGRESS", rule.getProcessStatus());
    }

    @Test
    @DisplayName("Changing type is allowed when endpoints stay consistent")
    void setTypeConsistent() {
        WorkflowRule rule = new WorkflowRule(RuleType.TASK_TO_END, a, null, null, "END");
        rule.setType(RuleType.TASK_TO_SPLIT_TO_END);
        assertEquals(RuleType.TASK_TO_SPLIT_TO_END, rule.getType());
        assertTrue(rule.isFinal());
    }

    @Test
    @DisplayName("endpointsValid never throws")
    void endpointsValidIsSafe() {
        assertFalse(WorkflowRule.endpointsValid(null, a, b));
        assertTrue(WorkflowRule.endpointsValid(RuleType.TASK_TO_TASK, a, b));
        assertFalse(WorkflowRule.endpointsValid(RuleType.TASK_TO_TASK, null, b));
    }
}
