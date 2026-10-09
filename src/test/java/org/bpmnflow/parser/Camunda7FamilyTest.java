package org.bpmnflow.parser;

import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.ApiActivityNode;
import org.bpmnflow.model.ApiField;
import org.bpmnflow.model.ApiHandlerDefinition;
import org.bpmnflow.model.Workflow;
import org.bpmnflow.parser.engine.EngineAdapterFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Camunda 7 family: Camunda 7, CIB seven (camunda: namespace) and
 * Operaton (operaton: namespace, camunda: still accepted).
 */
@DisplayName("Camunda 7 family: Camunda 7, CIB seven and Operaton models")
class Camunda7FamilyTest {

    private static Workflow parse(String model, String engine) {
        try (InputStream m = Camunda7FamilyTest.class.getResourceAsStream(model);
             InputStream c = Camunda7FamilyTest.class.getResourceAsStream("/config/test_config_api_c7.yaml")) {
            assertNotNull(m, "Model not found: " + model);
            BpmnPropertiesConfig config = ConfigLoader.loadConfig(c);
            config.setEngine(engine);
            return ModelParser.parser(m, config);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ApiActivityNode api(Workflow workflow, String activityCode) {
        ActivityNode node = workflow.getActivities().stream()
                .filter(a -> activityCode.equals(a.getActivityCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Activity not found: " + activityCode));
        return assertInstanceOf(ApiActivityNode.class, node);
    }

    /**
     * The three models describe the same process; whatever the namespace,
     * the parse result must be identical to the Camunda 7 reference.
     */
    @ParameterizedTest(name = "{0} with engine={1}")
    @CsvSource({
            "/models/model_api_c7.bpmn,       camunda7",
            "/models/model_api_c7.bpmn,       cibseven",
            "/models/model_api_operaton.bpmn, operaton",
            "/models/model_api_mixed_ns.bpmn, operaton",
            "/models/model_api_operaton.bpmn, camunda7"
    })
    @DisplayName("Same process, any namespace → identical result")
    void sameResultForEveryNamespace(String model, String engine) {
        Workflow wf = parse(model, engine);

        assertAll(
                () -> assertEquals(0, wf.inconsistenciesSize(), () -> wf.getInconsistencies().toString()),
                () -> assertEquals("Process_api_c7", wf.getId()),
                () -> assertEquals("1.0", wf.getVersion(), "versionTag"),
                () -> assertEquals("FD", wf.getType()),
                () -> assertEquals("DLV", wf.getSubtype()),
                () -> assertEquals(1, wf.stagesSize()),
                () -> assertEquals(3, wf.activitiesSize()),
                () -> assertEquals(4, wf.rulesSize())
        );

        ApiHandlerDefinition payment = api(wf, "PMT_AUTH").getApiHandler();
        assertAll(
                () -> assertEquals("http-connector", payment.getConnectorId()),
                () -> assertEquals("https://api.pagamentos.com/v1/authorize", payment.getEndpoint()),
                () -> assertEquals("POST", payment.getMethod()),
                () -> assertEquals(List.of("x-api-version"),
                        payment.getTaskHeaders().stream().map(ApiField::getKey).toList()),
                () -> assertEquals(List.of("payload"),
                        payment.getInputMappings().stream().map(ApiField::getKey).toList()),
                () -> assertEquals(List.of("pagamento_txn_id", "pagamento_status"),
                        payment.getOutputMappings().stream().map(ApiField::getKey).toList())
        );
    }

    @Nested
    @DisplayName("Engine aliases")
    class Aliases {

        @ParameterizedTest
        @ValueSource(strings = {"operaton", "cibseven"})
        @DisplayName("Alias is a supported engine and reports its own id")
        void aliasSupported(String engine) {
            BpmnPropertiesConfig config = new BpmnPropertiesConfig();
            config.setEngine(engine);

            assertTrue(EngineAdapterFactory.supportedEngines().contains(engine));
            assertEquals(engine, EngineAdapterFactory.create(config).engineId());
        }

        @ParameterizedTest
        @ValueSource(strings = {"operaton", "cibseven"})
        @DisplayName("Alias is accepted in the YAML 'engine' field")
        void aliasAcceptedInYaml(String engine) {
            String yaml = """
                    bpmn_model_parser:
                      engine: %s
                      model_properties:
                        task:
                          - name: stage
                            required: true
                            extension: true
                    """.formatted(engine);

            BpmnPropertiesConfig config = ConfigLoader.loadConfig(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

            assertEquals(engine, config.getEngine());
        }

        @Test
        @DisplayName("Default Camunda 7 adapter still reports 'camunda7'")
        void defaultId() {
            assertEquals("camunda7", new BpmnPropertiesConfig().getEngine());
            assertEquals("camunda7", EngineAdapterFactory.create(new BpmnPropertiesConfig()).engineId());
        }
    }
}
