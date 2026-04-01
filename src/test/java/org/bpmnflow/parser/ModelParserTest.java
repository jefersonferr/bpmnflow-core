package org.bpmnflow;

import org.bpmnflow.model.Workflow;
import org.bpmnflow.parser.ConfigLoader;
import org.bpmnflow.parser.ModelParser;
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
    // Carrega modelo e config do classpath via getResourceAsStream.
    // Funciona em qualquer OS, qualquer working directory, qualquer CI.
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
    // Model 01 — consistente, 3 regras.
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
        @DisplayName("Estrutura geral: 0 stages, 0 inconsistências, 2 atividades, 3 regras")
        void estruturaGeral() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(0, workflow.inconsistenciesSize()),
                    () -> assertEquals(2, workflow.activitiesSize()),
                    () -> assertEquals(3, workflow.rulesSize())
            );
        }

        @Test
        @DisplayName("Rule 1: StartEvent → Task — source null, target presente, sem conclusão")
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
        @DisplayName("Rule 2: Task → Task — source e target presentes, sem conclusão ou status")
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
        @DisplayName("Rule 7: Task → EndEvent — source presente, target null, status presente")
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
    // Model 02 — inconsistências 10, 11 e 12.
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
        @DisplayName("Estrutura geral: 0 stages, 4 inconsistências, 0 atividades, 0 regras")
        void estruturaGeral() {
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
    // Model 03 — consistente, 5 regras, com gateways split/merge.
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
        @DisplayName("Estrutura geral: 0 stages, 0 inconsistências, 2 atividades, 5 regras")
        void estruturaGeral() {
            assertAll(
                    () -> assertEquals(0, workflow.stagesSize()),
                    () -> assertEquals(0, workflow.inconsistenciesSize()),
                    () -> assertEquals(2, workflow.activitiesSize()),
                    () -> assertEquals(5, workflow.rulesSize())
            );
        }

        @Test
        @DisplayName("Rule 5: Split → Task — source, target e conclusão presentes")
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
        @DisplayName("Rule 8: Task → Split → EndEvent — source, conclusão e status presentes")
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
    // Model 04 — inconsistência 14.
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
        @DisplayName("Estrutura geral: 0 stages, 3 inconsistências, 2 atividades, 2 regras")
        void estruturaGeral() {
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
    // Model 05 — inconsistências 1, 3, 5, 6, 101, 102.
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
        @DisplayName("Estrutura geral: 0 stages, 6 inconsistências, 2 atividades, 5 regras")
        void estruturaGeral() {
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
    // Model 06 — inconsistências 7, 8, 9, 13.
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
        @DisplayName("Estrutura geral: 0 stages, 6 inconsistências, 1 atividade, 4 regras")
        void estruturaGeral() {
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
    // Model 07 — config relaxado (test_config_03).
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
        @DisplayName("Estrutura geral: 0 stages, 0 inconsistências, 2 atividades, 5 regras")
        void estruturaGeral() {
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
    // Model 08 — modelo real com 14 atividades, Rule 04 e Rule 06.
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
        @DisplayName("Estrutura geral: 7 stages, 0 inconsistências, 14 atividades, 32 regras")
        void estruturaGeral() {
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
        @DisplayName("Estrutura geral: 3 stages, 0 inconsistências, 4 atividades, 7 regras")
        void estruturaGeral() {
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
}
