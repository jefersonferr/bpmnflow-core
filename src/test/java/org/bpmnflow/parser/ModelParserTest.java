package org.bpmnflow.parser;

import org.bpmnflow.model.Conclusion;
import org.bpmnflow.model.Workflow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestClassOrder(ClassOrderer.DisplayName.class)
class ModelParserTest {

    static final PrintStream OUT;
    static {
        try {
            OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------
    // Loads model and config from classpath via getResourceAsStream.
    // Works on any OS, any working directory, any CI environment.
    // ------------------------------------------------------------------
    private static Workflow parse(String modelResource, String configResource) {
        try (InputStream model  = ModelParserTest.class.getResourceAsStream(modelResource);
             InputStream config = ModelParserTest.class.getResourceAsStream(configResource)) {

            if (model == null)
                throw new IllegalArgumentException("Model not found on classpath: " + modelResource);
            if (config == null)
                throw new IllegalArgumentException("Config not found on classpath: " + configResource);

            return ModelParser.parser(model, ConfigLoader.loadConfig(config));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse: " + modelResource, e);
        }
    }

    // ------------------------------------------------------------------
    // Model 01 — consistent, 3 rules.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 01: Consistent")
    class Model01Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_01.bpmn", "/config/test_config_01.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 stages, 0 inconsistencies, 2 activities, 3 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(0, workflow.inconsistenciesSize()),
                    () -> assertEquals(2, workflow.activitiesSize()),
                    () -> assertEquals(3, workflow.rulesSize())
            );
        }

        @Test
        @DisplayName("Rule 1: StartEvent → Task — source null, target present, no conclusion")
        void rule1_startEventToTask() {
            var rule = workflow.getRules().get(0);
            assertAll(
                    () -> assertNull(rule.getSource()),
                    () -> assertNotNull(rule.getTarget()),
                    () -> assertNull(rule.getConclusion()),
                    () -> assertNotNull(rule.getProcessStatus())
            );
        }

        @Test
        @DisplayName("Rule 2: Task → Task — source and target present, no conclusion or status")
        void rule2_taskToTask() {
            var rule = workflow.getRules().get(workflow.getRules().size() - 1);
            assertAll(
                    () -> assertNotNull(rule.getSource()),
                    () -> assertNotNull(rule.getTarget()),
                    () -> assertNull(rule.getConclusion()),
                    () -> assertNull(rule.getProcessStatus())
            );
        }

        @Test
        @DisplayName("Rule 7: Task → EndEvent — source present, target null, status present")
        void rule7_taskToEndEvent() {
            var rule = workflow.getRules().get(1);
            assertAll(
                    () -> assertNotNull(rule.getSource()),
                    () -> assertNull(rule.getTarget()),
                    () -> assertNull(rule.getConclusion()),
                    () -> assertNotNull(rule.getProcessStatus())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 01: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 02 — inconsistencies 10, 11 and 12.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 02: Inconsistencies 10, 11 and 12")
    class Model02Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_02.bpmn", "/config/test_config_01.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 stages, 4 inconsistencies, 0 activities, 0 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(4, workflow.inconsistenciesSize()),
                    () -> assertEquals(0, workflow.activitiesSize()),
                    () -> assertEquals(0, workflow.rulesSize())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 02: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 03 — consistent, 5 rules, split/merge gateways.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 03: Consistent — split/merge gateways")
    class Model03Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_03.bpmn", "/config/test_config_01.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 stages, 0 inconsistencies, 2 activities, 5 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(0, workflow.inconsistenciesSize()),
                    () -> assertEquals(2, workflow.activitiesSize()),
                    () -> assertEquals(5, workflow.rulesSize())
            );
        }

        @Test
        @DisplayName("Rule 5: Split → Task — source, target and conclusion present")
        void rule5_splitToTask() {
            var rule = workflow.getRules().get(2);
            assertAll(
                    () -> assertNotNull(rule.getSource()),
                    () -> assertNotNull(rule.getTarget()),
                    () -> assertNotNull(rule.getConclusion()),
                    () -> assertNull(rule.getProcessStatus())
            );
        }

        @Test
        @DisplayName("Rule 8: Task → Split → EndEvent — source, conclusion and status present")
        void rule8_taskSplitToEndEvent() {
            var rule = workflow.getRules().get(4);
            assertAll(
                    () -> assertNotNull(rule.getSource()),
                    () -> assertNull(rule.getTarget()),
                    () -> assertNotNull(rule.getConclusion()),
                    () -> assertNotNull(rule.getProcessStatus())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 03: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 04 — inconsistency 14.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 04: Inconsistency 14")
    class Model04Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_04.bpmn", "/config/test_config_01.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 stages, 3 inconsistencies, 2 activities, 2 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(3, workflow.inconsistenciesSize()),
                    () -> assertEquals(2, workflow.activitiesSize()),
                    () -> assertEquals(2, workflow.rulesSize())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 04: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 05 — inconsistencies 1, 3, 5, 6, 101, 102.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 05: Inconsistencies 1, 3, 5, 6, 101, 102")
    class Model05Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_05.bpmn", "/config/test_config_02.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 stages, 6 inconsistencies, 2 activities, 5 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(6, workflow.inconsistenciesSize()),
                    () -> assertEquals(2, workflow.activitiesSize()),
                    () -> assertEquals(5, workflow.rulesSize())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 05: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 06 — inconsistencies 7, 8, 9, 13.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 06: Inconsistencies 7, 8, 9, 13 (process_type/subtype read from participant)")
    class Model06Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_06.bpmn", "/config/test_config_02.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 stages, 6 inconsistencies, 1 activity, 4 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(6, workflow.inconsistenciesSize()),
                    () -> assertEquals(1, workflow.activitiesSize()),
                    () -> assertEquals(4, workflow.rulesSize())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 06: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 07 — relaxed config (test_config_03).
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 07: Relaxed config requirements")
    class Model07Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_07.bpmn", "/config/test_config_03.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 stages, 0 inconsistencies, 2 activities, 5 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(0, workflow.inconsistenciesSize()),
                    () -> assertEquals(2, workflow.activitiesSize()),
                    () -> assertEquals(5, workflow.rulesSize())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 07: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 08 — real-world model with 14 activities, Rule 04 and Rule 06.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 08: Rule 04 (Task → Merge → End) and Rule 06 (Split → Merge)")
    class Model08Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_08.bpmn", "/config/test_config_03.yaml");
        }

        @Test
        @DisplayName("Overall structure: 7 stages, 0 inconsistencies, 14 activities, 32 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(7, workflow.stagesSize()),
                    () -> assertEquals(0, workflow.inconsistenciesSize()),
                    () -> assertEquals(14, workflow.activitiesSize()),
                    () -> assertEquals(32, workflow.rulesSize())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 08: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 09 — Rule 03 (Task → Merge → EndEvent).
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 09: Rule 03 (Task → Merge → EndEvent)")
    class Model09Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_09.bpmn", "/config/test_config_01.yaml");
        }

        @Test
        @DisplayName("Overall structure: 3 stages, 0 inconsistencies, 4 activities, 7 rules")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(3, workflow.stagesSize()),
                    () -> assertEquals(0, workflow.inconsistenciesSize()),
                    () -> assertEquals(4, workflow.activitiesSize()),
                    () -> assertEquals(7, workflow.rulesSize())
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 09: " + workflow);
        }
    }

    // ------------------------------------------------------------------
    // Model 10 — merge+split gateway: GatewayHandler + RuleHandler regression.
    //
    // Gateway_order_valid has 2 incoming edges (from Task_receive_order and
    // Task_calm) and 2 outgoing edges with conclusions (NEEDS_ATTENTION,
    // ORDER_CONFIRMED).  Before the fix, the size-1 guard discarded the gateway
    // entirely, leaving Task_receive_order.conclusions empty and producing no
    // rules for the split paths.
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model 10: merge+split gateway — GatewayHandler + RuleHandler regression")
    class Model10Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_10.bpmn", "/config/test_config_04.yaml");
        }

        @Test
        @DisplayName("Overall structure: 0 inconsistencies, 2 activities")
        void overallStructure() {
            assertAll(
                    () -> assertEquals(0,  workflow.inconsistenciesSize()),
                    () -> assertEquals(2,  workflow.activitiesSize())
            );
        }

        @Test
        @DisplayName("SC-RCV (Receive Order) has 2 conclusions: NEEDS_ATTENTION and ORDER_CONFIRMED")
        void receiveOrderConclusions() {
            var rcv = workflow.getActivities().stream()
                    .filter(a -> "SC-RCV".equals(a.getAbbreviation()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("SC-RCV not found"));

            var codes = rcv.getConclusions().stream()
                    .map(Conclusion::getCode)
                    .toList();

            assertAll(
                    () -> assertEquals(2, rcv.getConclusions().size(),
                            "SC-RCV must have exactly 2 conclusions"),
                    () -> assertTrue(codes.contains("NEEDS_ATTENTION"),
                            "SC-RCV must contain NEEDS_ATTENTION"),
                    () -> assertTrue(codes.contains("ORDER_CONFIRMED"),
                            "SC-RCV must contain ORDER_CONFIRMED")
            );
        }

        @Test
        @DisplayName("SC-CLM (Call to Customer) has 2 conclusions — shared merge+split predecessor")
        void callToCustomerConclusions() {
            var clm = workflow.getActivities().stream()
                    .filter(a -> "SC-CLM".equals(a.getAbbreviation()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("SC-CLM not found"));

            var codes = clm.getConclusions().stream()
                    .map(Conclusion::getCode)
                    .toList();

            assertAll(
                    () -> assertEquals(2, clm.getConclusions().size(),
                            "SC-CLM must have exactly 2 conclusions — same as SC-RCV (shared gateway)"),
                    () -> assertTrue(codes.contains("NEEDS_ATTENTION"),
                            "SC-CLM must contain NEEDS_ATTENTION"),
                    () -> assertTrue(codes.contains("ORDER_CONFIRMED"),
                            "SC-CLM must contain ORDER_CONFIRMED")
            );
        }

        @Test
        @DisplayName("Rule 5 (SPLIT_TO_TASK): SC-RCV → SC-CLM and SC-CLM → SC-CLM with NEEDS_ATTENTION")
        void rule5_splitToTask() {
            var rules = workflow.getRules().stream()
                    .filter(r -> r.getType().getCode() == 5
                            && "NEEDS_ATTENTION".equals(r.getConclusion() != null
                            ? r.getConclusion().getCode() : null))
                    .toList();

            assertEquals(2, rules.size(),
                    "Must have 2 SPLIT_TO_TASK rules with NEEDS_ATTENTION — one per predecessor");

            var sources = rules.stream()
                    .map(r -> r.getSource().getAbbreviation())
                    .toList();

            assertAll(
                    () -> assertTrue(sources.contains("SC-RCV"),
                            "SC-RCV must be a source of NEEDS_ATTENTION SPLIT_TO_TASK"),
                    () -> assertTrue(sources.contains("SC-CLM"),
                            "SC-CLM must be a source of NEEDS_ATTENTION SPLIT_TO_TASK (loop)")
            );
        }

        @Test
        @DisplayName("Rule 8 (TASK_TO_SPLIT_TO_END): SC-RCV and SC-CLM → EndEvent with ORDER_CONFIRMED")
        void rule8_splitToEnd() {
            var rules = workflow.getRules().stream()
                    .filter(r -> r.getType().getCode() == 8)
                    .toList();

            assertEquals(2, rules.size(),
                    "Must have 2 TASK_TO_SPLIT_TO_END rules — one per predecessor");

            var sources = rules.stream()
                    .map(r -> r.getSource().getAbbreviation())
                    .toList();

            assertAll(
                    () -> assertTrue(sources.contains("SC-RCV"),
                            "SC-RCV must have TASK_TO_SPLIT_TO_END with ORDER_CONFIRMED"),
                    () -> assertTrue(sources.contains("SC-CLM"),
                            "SC-CLM must have TASK_TO_SPLIT_TO_END with ORDER_CONFIRMED"),
                    () -> rules.forEach(r -> assertEquals("ORDER_CONFIRMED",
                            r.getConclusion().getCode())),
                    () -> rules.forEach(r -> assertEquals("CLOSED",
                            r.getProcessStatus()))
            );
        }

        @AfterAll
        void printWorkflow() {
            OUT.println("Model 10: " + workflow);
        }
    }
}