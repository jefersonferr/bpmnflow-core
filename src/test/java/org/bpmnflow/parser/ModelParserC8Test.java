package org.bpmnflow.parser;

import org.bpmnflow.model.Workflow;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes para parsing de modelos Camunda 8 (namespace zeebe:).
 *
 * <p>Espelho de {@link ModelParserTest} com:
 * <ul>
 *   <li>engine: camunda8 no config</li>
 *   <li>Modelo BPMN com elementos zeebe: em vez de camunda:</li>
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
                    "Modelo não encontrado no classpath: " + modelResource);
            if (config == null) throw new IllegalArgumentException(
                    "Config não encontrado no classpath: " + configResource);

            return ModelParser.parser(model, ConfigLoader.loadConfig(config));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao parsear: " + modelResource, e);
        }
    }

    // ------------------------------------------------------------------
    // Model C8 01 — consistente, engine camunda8
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
        @DisplayName("Parse não deve lançar exceção com engine camunda8")
        void parseWithoutException() {
            assertNotNull(workflow);
        }

        @Test
        @DisplayName("Estrutura geral: 0 inconsistências, 1 stage, 2 atividades, 3 regras")
        void estruturaGeral() {
            OUT.println(workflow);
            assertAll(
                    () -> assertEquals(0, workflow.inconsistenciesSize(),
                            "Não deve haver inconsistências"),
                    () -> assertEquals(1, workflow.stagesSize(),
                            "Deve ter 1 stage (lane)"),
                    () -> assertEquals(2, workflow.activitiesSize(),
                            "Deve ter 2 atividades"),
                    () -> assertEquals(3, workflow.rulesSize(),
                            "Deve ter 3 regras")
            );
        }

        @Test
        @DisplayName("Version tag extraído via zeebe:versionTag (elemento filho)")
        void versionTagC8() {
            assertEquals("1.0", workflow.getVersion(),
                    "Version tag deve ser '1.0' lido de <zeebe:versionTag value='1.0'/>");
        }

        @Test
        @DisplayName("Workflow name extraído do Participant")
        void workflowName() {
            assertEquals("Test Process C8", workflow.getName());
        }

        @Test
        @DisplayName("Workflow id extraído do Process")
        void workflowId() {
            assertEquals("Process_c8_01", workflow.getId());
        }

        @Test
        @DisplayName("process_type e process_subtype extraídos via zeebe:property do Participant")
        void processTypeAndSubtype() {
            assertAll(
                    () -> assertEquals("TEST_TYPE",    workflow.getType()),
                    () -> assertEquals("TEST_SUBTYPE", workflow.getSubtype())
            );
        }

        @Test
        @DisplayName("Stage da lane extraído via zeebe:property name='stage'")
        void laneStageCode() {
            assertEquals("AN", workflow.getStages().get(0).getCode());
        }

        @Test
        @DisplayName("Activities devem ter stage e activity code preenchidos via zeebe:property")
        void activitiesHaveStageAndCode() {
            workflow.getActivities().forEach(a -> assertAll(
                    () -> assertNotNull(a.getStageCode(),
                            "stage não deve ser null em " + a.getName()),
                    () -> assertNotNull(a.getActivityCode(),
                            "activity não deve ser null em " + a.getName())
            ));
        }

        @Test
        @DisplayName("Rule 1: StartEvent → Task — source null, target presente, status NEW")
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
        @DisplayName("Rule 2: Task → Task — source e target presentes, sem conclusão")
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
    // Validação do campo engine no ConfigLoader
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("ConfigLoader: validação do campo engine")
    class EngineValidationTests {

        @Test
        @DisplayName("engine ausente no YAML → default camunda7, sem exceção")
        void missingEngineDefaultsToCamunda7() {
            BpmnPropertiesConfig config = ConfigLoader.loadConfig(
                    ModelParserC8Test.class.getResourceAsStream("/config/test_config_01.yaml"));
            assertNotNull(config);
            assertEquals("camunda7", config.getEngine());
        }

        @Test
        @DisplayName("engine: camunda8 carregado corretamente")
        void camunda8EngineLoadedCorrectly() {
            BpmnPropertiesConfig config = ConfigLoader.loadConfig(
                    ModelParserC8Test.class.getResourceAsStream("/config/test_config_c8_01.yaml"));
            assertEquals("camunda8", config.getEngine());
        }

        @Test
        @DisplayName("engine com valor inválido → BpmnConfigException")
        void invalidEngineThrowsException() {
            BpmnPropertiesConfig config = new BpmnPropertiesConfig();
            config.setEngine("flowable");
            assertThrows(
                    BpmnConfigException.class,
                    () -> org.bpmnflow.parser.engine.EngineAdapterFactory.create(config),
                    "Engine não suportado deve lançar BpmnConfigException"
            );
        }

        @Test
        @DisplayName("EngineAdapterFactory.supportedEngines() retorna camunda7 e camunda8")
        void supportedEnginesContainsBothVersions() {
            var engines = org.bpmnflow.parser.engine.EngineAdapterFactory.supportedEngines();
            assertAll(
                    () -> assertTrue(engines.contains("camunda7")),
                    () -> assertTrue(engines.contains("camunda8"))
            );
        }
    }
}
