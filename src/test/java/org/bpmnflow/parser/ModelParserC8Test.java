package org.bpmnflow.parser;

import org.bpmnflow.model.Workflow;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for parsing Camunda 8 models (zeebe: namespace).
 *
 * <p>Mirror of {@link ModelParserTest} with:
 * <ul>
 *   <li>engine: camunda8 in config</li>
 *   <li>BPMN model using zeebe: elements instead of camunda:</li>
 * </ul>
 */
@TestClassOrder(ClassOrderer.DisplayName.class)
class ModelParserC8Test {

    static final PrintStream OUT;
    static {
        try {
            OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static Workflow parse(String modelResource, String configResource) {
        try (InputStream model  = ModelParserC8Test.class.getResourceAsStream(modelResource);
             InputStream config = ModelParserC8Test.class.getResourceAsStream(configResource)) {

            if (model  == null) throw new IllegalArgumentException(
                    "Model not found on classpath: " + modelResource);
            if (config == null) throw new IllegalArgumentException(
                    "Config not found on classpath: " + configResource);

            return ModelParser.parser(model, ConfigLoader.loadConfig(config));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse: " + modelResource, e);
        }
    }

    // ------------------------------------------------------------------
    // Model C8 01 — consistent, engine camunda8
    // ------------------------------------------------------------------
    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Model C8 01: Consistent — zeebe namespace")
    class ModelC8_01Tests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_c8_01.bpmn", "/config/test_config_c8_01.yaml");
        }

        @Test
        @DisplayName("Parse should not throw with engine camunda8")
        void parseWithoutException() {
            assertNotNull(workflow);
        }

        @Test
        @DisplayName("Overall structure: 0 inconsistencies, 1 stage, 2 activities, 3 rules")
        void overallStructure() {
            OUT.println(workflow);
            assertAll(
                    () -> assertEquals(0, workflow.inconsistenciesSize(),
                            "Should have no inconsistencies"),
                    () -> assertEquals(1, workflow.stagesSize(),
                            "Should have 1 stage (lane)"),
                    () -> assertEquals(2, workflow.activitiesSize(),
                            "Should have 2 activities"),
                    () -> assertEquals(3, workflow.rulesSize(),
                            "Should have 3 rules")
            );
        }

        @Test
        @DisplayName("Version tag extracted via zeebe:versionTag (child element)")
        void versionTagC8() {
            assertEquals("1.0", workflow.getVersion(),
                    "Version tag should be '1.0' read from <zeebe:versionTag value='1.0'/>");
        }

        @Test
        @DisplayName("Workflow name extracted from Participant")
        void workflowName() {
            assertEquals("Test Process C8", workflow.getName());
        }

        @Test
        @DisplayName("Workflow id extracted from Process")
        void workflowId() {
            assertEquals("Process_c8_01", workflow.getId());
        }

        @Test
        @DisplayName("process_type and process_subtype extracted via zeebe:property from Participant")
        void processTypeAndSubtype() {
            assertAll(
                    () -> assertEquals("TEST_TYPE",    workflow.getType()),
                    () -> assertEquals("TEST_SUBTYPE", workflow.getSubtype())
            );
        }

        @Test
        @DisplayName("Lane stage extracted via zeebe:property name='stage'")
        void laneStageCode() {
            assertEquals("AN", workflow.getStages().get(0).getCode());
        }

        @Test
        @DisplayName("Activities should have stage and activity code populated via zeebe:property")
        void activitiesHaveStageAndCode() {
            workflow.getActivities().forEach(a -> assertAll(
                    () -> assertNotNull(a.getStageCode(),
                            "stage must not be null in " + a.getName()),
                    () -> assertNotNull(a.getActivityCode(),
                            "activity must not be null in " + a.getName())
            ));
        }

        @Test
        @DisplayName("Rule 1: StartEvent → Task — source null, target present, status NEW")
        void rule1_startEventToTask() {
            var rule = workflow.getRules().get(0);
            assertAll(
                    () -> assertNull(rule.getSource()),
                    () -> assertNotNull(rule.getTarget()),
                    () -> assertNull(rule.getConclusion()),
                    () -> assertEquals("NEW", rule.getProcessStatus())
            );
        }

        @Test
        @DisplayName("Rule 2: Task → Task — source and target present, no conclusion")
        void rule2_taskToTask() {
            var rule = workflow.getRules().get(1);
            assertAll(
                    () -> assertNotNull(rule.getSource()),
                    () -> assertNotNull(rule.getTarget()),
                    () -> assertNull(rule.getConclusion()),
                    () -> assertNull(rule.getProcessStatus())
            );
        }

        @Test
        @DisplayName("Rule 3: Task → EndEvent — target null, status DONE")
        void rule3_taskToEnd() {
            var rule = workflow.getRules().get(2);
            assertAll(
                    () -> assertNotNull(rule.getSource()),
                    () -> assertNull(rule.getTarget()),
                    () -> assertNull(rule.getConclusion()),
                    () -> assertEquals("DONE", rule.getProcessStatus())
            );
        }
    }

    // ------------------------------------------------------------------
    // Engine field validation in ConfigLoader
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("ConfigLoader: engine field validation")
    class EngineValidationTests {

        @Test
        @DisplayName("engine absent from YAML → default camunda7, no exception")
        void missingEngineDefaultsToCamunda7() {
            BpmnPropertiesConfig config = ConfigLoader.loadConfig(
                    ModelParserC8Test.class.getResourceAsStream("/config/test_config_01.yaml"));
            assertNotNull(config);
            assertEquals("camunda7", config.getEngine());
        }

        @Test
        @DisplayName("engine: camunda8 loaded correctly")
        void camunda8EngineLoadedCorrectly() {
            BpmnPropertiesConfig config = ConfigLoader.loadConfig(
                    ModelParserC8Test.class.getResourceAsStream("/config/test_config_c8_01.yaml"));
            assertEquals("camunda8", config.getEngine());
        }

        @Test
        @DisplayName("engine with invalid value → BpmnConfigException")
        void invalidEngineThrowsException() {
            BpmnPropertiesConfig config = new BpmnPropertiesConfig();
            config.setEngine("flowable");
            assertThrows(
                    BpmnConfigException.class,
                    () -> org.bpmnflow.parser.engine.EngineAdapterFactory.create(config),
                    "Unsupported engine should throw BpmnConfigException"
            );
        }

        @Test
        @DisplayName("EngineAdapterFactory.supportedEngines() returns camunda7 and camunda8")
        void supportedEnginesContainsBothVersions() {
            var engines = org.bpmnflow.parser.engine.EngineAdapterFactory.supportedEngines();
            assertAll(
                    () -> assertTrue(engines.contains("camunda7")),
                    () -> assertTrue(engines.contains("camunda8"))
            );
        }
    }
}
