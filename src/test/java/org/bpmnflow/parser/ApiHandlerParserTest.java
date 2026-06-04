package org.bpmnflow.parser;

import org.bpmnflow.model.ApiActivityNode;
import org.bpmnflow.model.ApiHandlerDefinition;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.Workflow;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ServiceTask parsing via camunda:connector (C7) and
 * zeebe:taskDefinition (C8). Validates that FlowNodeHandler creates
 * ApiActivityNode instances and that the EngineAdapters correctly
 * populate ApiHandlerDefinition fields.
 */
@TestClassOrder(ClassOrderer.DisplayName.class)
class ApiHandlerParserTest {

    static final PrintStream OUT;
    static {
        try {
            OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Workflow parse(String modelResource, String configResource) {
        try (InputStream model  = ApiHandlerParserTest.class.getResourceAsStream(modelResource);
             InputStream config = ApiHandlerParserTest.class.getResourceAsStream(configResource)) {

            if (model  == null) throw new IllegalArgumentException("Model not found: "  + modelResource);
            if (config == null) throw new IllegalArgumentException("Config not found: " + configResource);

            return ModelParser.parser(model, ConfigLoader.loadConfig(config));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse: " + modelResource, e);
        }
    }

    // ================================================================
    // Camunda 7 — camunda:connector
    // ================================================================
    @Nested
    @DisplayName("C7: camunda:connector -> ApiActivityNode")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Camunda7ApiTests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_api_c7.bpmn", "/config/test_config_api_c7.yaml");
            OUT.println(workflow);
        }

        @Test
        @DisplayName("Parse produces 0 inconsistencies")
        void noInconsistencies() {
            assertEquals(0, workflow.inconsistenciesSize(),
                    () -> "Unexpected inconsistencies: " + workflow.getInconsistencies());
        }

        @Test
        @DisplayName("Workflow has 3 activities (2 ApiActivityNode + 1 plain ActivityNode)")
        void activityCount() {
            assertEquals(3, workflow.activitiesSize());
        }

        @Test
        @DisplayName("Task_api_payment_c7 is an ApiActivityNode")
        void paymentTaskIsApiNode() {
            ActivityNode node = findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node, "PMT_AUTH activity not found");
            assertInstanceOf(ApiActivityNode.class, node,
                    "Expected ApiActivityNode for ServiceTask PMT_AUTH");
        }

        @Test
        @DisplayName("Payment ApiHandlerDefinition: connectorId, endpoint, method populated")
        void paymentApiHandlerFields() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            assertAll(
                    () -> assertEquals("http-connector", def.getConnectorId()),
                    () -> assertEquals("https://api.pagamentos.com/v1/authorize", def.getEndpoint()),
                    () -> assertEquals("POST", def.getMethod()),
                    () -> assertEquals(0, def.getRetries(), "C7 retries should default to 0")
            );
        }

        @Test
        @DisplayName("Payment ApiHandlerDefinition: outputMappings contains pagamento_txn_id and pagamento_status")
        void paymentOutputMappings() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            assertFalse(def.getOutputMappings().isEmpty(), "outputMappings should not be empty");
            boolean hasTxnId = def.getOutputMappings().stream()
                    .anyMatch(f -> "pagamento_txn_id".equals(f.getKey()));
            boolean hasStatus = def.getOutputMappings().stream()
                    .anyMatch(f -> "pagamento_status".equals(f.getKey()));
            assertAll(
                    () -> assertTrue(hasTxnId,  "pagamento_txn_id missing from outputMappings"),
                    () -> assertTrue(hasStatus, "pagamento_status missing from outputMappings")
            );
        }

        @Test
        @DisplayName("Payment ApiHandlerDefinition: taskHeaders contains extra inputParameter x-api-version")
        void paymentTaskHeaders() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            boolean hasApiVersion = def.getTaskHeaders().stream()
                    .anyMatch(f -> "x-api-version".equals(f.getKey()));
            assertTrue(hasApiVersion, "x-api-version missing from taskHeaders");
        }

        @Test
        @DisplayName("Task_api_tracking_c7 is an ApiActivityNode")
        void trackingTaskIsApiNode() {
            ActivityNode node = findByActivity(workflow, "TRK_CREATE");
            assertNotNull(node, "TRK_CREATE activity not found");
            assertInstanceOf(ApiActivityNode.class, node);
        }

        @Test
        @DisplayName("Tracking ApiHandlerDefinition: endpoint and method populated")
        void trackingApiHandlerFields() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "TRK_CREATE");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            assertAll(
                    () -> assertEquals("https://api.logistica.com/v1/deliveries", def.getEndpoint()),
                    () -> assertEquals("POST", def.getMethod())
            );
        }

        @Test
        @DisplayName("Task_receive_order_c7 is a plain ActivityNode (not ApiActivityNode)")
        void plainTaskRemainsActivityNode() {
            ActivityNode node = findByActivity(workflow, "RCV");
            assertNotNull(node, "RCV activity not found");
            assertFalse(node instanceof ApiActivityNode,
                    "Plain Task should NOT be an ApiActivityNode");
        }

        @Test
        @DisplayName("ApiActivityNode.toString() contains abbreviation and apiHandler")
        void apiActivityNodeToString() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            String str = node.toString();
            assertTrue(str.contains("SC-PMT_AUTH"), "toString should contain abbreviation");
            assertTrue(str.contains("ApiHandlerDefinition"), "toString should contain apiHandler");
        }

        @Test
        @DisplayName("ApiHandlerDefinition.toString() contains all key fields")
        void apiHandlerDefinitionToString() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            String str = node.getApiHandler().toString();
            assertTrue(str.contains("http-connector"));
            assertTrue(str.contains("api.pagamentos.com"));
            assertTrue(str.contains("POST"));
        }
    }

    // ================================================================
    // Camunda 8 — zeebe:taskDefinition
    // ================================================================
    @Nested
    @DisplayName("C8: zeebe:taskDefinition -> ApiActivityNode")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Camunda8ApiTests {

        Workflow workflow;

        @BeforeAll
        void parseModel() {
            workflow = parse("/models/model_api_c8.bpmn", "/config/test_config_api_c8.yaml");
            OUT.println(workflow);
        }

        @Test
        @DisplayName("Parse produces 0 inconsistencies")
        void noInconsistencies() {
            assertEquals(0, workflow.inconsistenciesSize(),
                    () -> "Unexpected inconsistencies: " + workflow.getInconsistencies());
        }

        @Test
        @DisplayName("Workflow has 3 activities (2 ApiActivityNode + 1 plain ActivityNode)")
        void activityCount() {
            assertEquals(3, workflow.activitiesSize());
        }

        @Test
        @DisplayName("Task_api_payment_c8 is an ApiActivityNode")
        void paymentTaskIsApiNode() {
            ActivityNode node = findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node, "PMT_AUTH activity not found");
            assertInstanceOf(ApiActivityNode.class, node);
        }

        @Test
        @DisplayName("Payment ApiHandlerDefinition: connectorId (type), endpoint, method, retries populated")
        void paymentApiHandlerFields() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            assertAll(
                    () -> assertEquals("pizza-delivery-payment-authorize", def.getConnectorId()),
                    () -> assertEquals("https://api.pagamentos.com/v1/authorize", def.getEndpoint()),
                    () -> assertEquals("POST", def.getMethod()),
                    () -> assertEquals(3, def.getRetries(), "C8 retries should be 3")
            );
        }

        @Test
        @DisplayName("Payment ApiHandlerDefinition: inputMappings contains cliente_id and valor_total")
        void paymentInputMappings() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            assertFalse(def.getInputMappings().isEmpty(), "inputMappings should not be empty");
            boolean hasCliente = def.getInputMappings().stream()
                    .anyMatch(f -> "cliente_id".equals(f.getKey()));
            boolean hasValor = def.getInputMappings().stream()
                    .anyMatch(f -> "valor_total".equals(f.getKey()));
            assertAll(
                    () -> assertTrue(hasCliente, "cliente_id missing from inputMappings"),
                    () -> assertTrue(hasValor,   "valor_total missing from inputMappings")
            );
        }

        @Test
        @DisplayName("Payment ApiHandlerDefinition: outputMappings contains pagamento_txn_id and pagamento_status")
        void paymentOutputMappings() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            assertFalse(def.getOutputMappings().isEmpty(), "outputMappings should not be empty");
            boolean hasTxnId = def.getOutputMappings().stream()
                    .anyMatch(f -> "pagamento_txn_id".equals(f.getKey()));
            boolean hasStatus = def.getOutputMappings().stream()
                    .anyMatch(f -> "pagamento_status".equals(f.getKey()));
            assertAll(
                    () -> assertTrue(hasTxnId,  "pagamento_txn_id missing from outputMappings"),
                    () -> assertTrue(hasStatus, "pagamento_status missing from outputMappings")
            );
        }

        @Test
        @DisplayName("Payment taskHeaders contains extra header x-api-version")
        void paymentTaskHeaders() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            boolean hasApiVersion = node.getApiHandler().getTaskHeaders().stream()
                    .anyMatch(f -> "x-api-version".equals(f.getKey()));
            assertTrue(hasApiVersion, "x-api-version missing from taskHeaders");
        }

        @Test
        @DisplayName("Tracking ApiHandlerDefinition: connectorId, endpoint, retries populated")
        void trackingApiHandlerFields() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "TRK_CREATE");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();

            assertAll(
                    () -> assertEquals("pizza-delivery-tracking-create", def.getConnectorId()),
                    () -> assertEquals("https://api.logistica.com/v1/deliveries", def.getEndpoint()),
                    () -> assertEquals(3, def.getRetries())
            );
        }

        @Test
        @DisplayName("Task_receive_order_c8 is a plain ActivityNode (not ApiActivityNode)")
        void plainTaskRemainsActivityNode() {
            ActivityNode node = findByActivity(workflow, "RCV");
            assertNotNull(node, "RCV activity not found");
            assertFalse(node instanceof ApiActivityNode,
                    "Plain Task should NOT be an ApiActivityNode");
        }

        @Test
        @DisplayName("Version tag extracted correctly via zeebe:versionTag")
        void versionTag() {
            assertEquals("1.0", workflow.getVersion());
        }

        @Test
        @DisplayName("ApiField.toString() renders key and value")
        void apiFieldToString() {
            ApiActivityNode node = (ApiActivityNode) findByActivity(workflow, "PMT_AUTH");
            assertNotNull(node);
            ApiHandlerDefinition def = node.getApiHandler();
            assertFalse(def.getInputMappings().isEmpty());
            String str = def.getInputMappings().get(0).toString();
            assertTrue(str.contains("ApiField{key="), "ApiField.toString() format incorrect");
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static ActivityNode findByActivity(Workflow workflow, String activityCode) {
        return workflow.getActivities().stream()
                .filter(a -> activityCode.equals(a.getActivityCode()))
                .findFirst()
                .orElse(null);
    }
}