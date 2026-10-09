package org.bpmnflow.parser;

import org.bpmnflow.model.RuleType;
import org.bpmnflow.model.Workflow;
import org.bpmnflow.model.WorkflowRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Model 12: Rule 6 (Split → Merge) with a merge+split gateway")
class SplitToMergeRuleTest {

    private Workflow workflow;

    @BeforeAll
    void parseModel() throws Exception {
        try (InputStream model = getClass().getResourceAsStream("/models/model_12.bpmn");
             InputStream config = getClass().getResourceAsStream("/config/test_config_03.yaml")) {
            workflow = ModelParser.parser(model, ConfigLoader.loadConfig(config));
        }
    }

    @Test
    @DisplayName("Overall structure: 0 inconsistencies, 3 activities, 6 rules")
    void overallStructure() {
        assertAll(
                () -> assertEquals(0, workflow.inconsistenciesSize(),
                        () -> workflow.getInconsistencies().toString()),
                () -> assertEquals(3, workflow.activitiesSize()),
                () -> assertEquals(6, workflow.rulesSize(), () -> workflow.getRules().toString())
        );
    }

    @Test
    @DisplayName("Both predecessors of the merge+split gateway get SPLIT_TO_MERGE → MS-AC3 (01)")
    void splitToMergeForEveryPredecessor() {
        List<String> splitToMerge = workflow.getRules().stream()
                .filter(r -> r.getType() == RuleType.SPLIT_TO_MERGE)
                .map(r -> r.getSource().getAbbreviation() + "->" + r.getTarget().getAbbreviation()
                        + ":" + r.getConclusion().getCode())
                .sorted()
                .toList();

        assertEquals(List.of("MS-AC1->MS-AC3:01", "MS-AC2->MS-AC3:01"), splitToMerge);
    }

    @Test
    @DisplayName("Retry branch: both predecessors get SPLIT_TO_TASK → MS-AC2 (02)")
    void retryBranch() {
        List<String> splitToTask = workflow.getRules().stream()
                .filter(r -> r.getType() == RuleType.SPLIT_TO_TASK)
                .map(r -> r.getSource().getAbbreviation() + "->" + r.getTarget().getAbbreviation()
                        + ":" + r.getConclusion().getCode())
                .sorted()
                .toList();

        assertEquals(List.of("MS-AC1->MS-AC2:02", "MS-AC2->MS-AC2:02"), splitToTask);
    }

    @Test
    @DisplayName("No rule with accidental null endpoints")
    void noAccidentalNulls() {
        for (WorkflowRule r : workflow.getRules()) {
            assertEquals(r.isInitial(), r.getSource() == null, () -> "source: " + r);
            assertEquals(r.isFinal(), r.getTarget() == null, () -> "target: " + r);
        }
    }

    @AfterAll
    void printWorkflow() {
        System.out.println("Model 12: " + workflow);
    }
}
