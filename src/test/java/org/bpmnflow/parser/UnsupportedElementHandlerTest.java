package org.bpmnflow.parser;

import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnsupportedElementHandler: reports elements BPMNFlow ignores")
class UnsupportedElementHandlerTest {

    private static BpmnModelInstance load(String resource) throws IOException {
        try (InputStream in = UnsupportedElementHandlerTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "Resource not found: " + resource);
            return Bpmn.readModelFromStream(in);
        }
    }

    @Test
    @DisplayName("Model 08: three boundary events are reported")
    void boundaryEventsReported() throws IOException {
        Map<String, List<String>> unsupported =
                UnsupportedElementHandler.findUnsupported(load("/models/model_08.bpmn"));

        assertEquals(Map.of("boundaryEvent",
                        List.of("Event_0aisjuz", "Event_1cz2fgx", "Event_02g55a5")),
                unsupported);
    }

    @Test
    @DisplayName("Model 01: only supported elements — nothing reported")
    void nothingReported() throws IOException {
        assertTrue(UnsupportedElementHandler.findUnsupported(load("/models/model_01.bpmn")).isEmpty());
    }

    @Test
    @DisplayName("Parallel gateway, sub-process, call activity and multi-instance are detected")
    void otherTypesDetected() {
        BpmnModelInstance model = Bpmn.createExecutableProcess("p")
                .startEvent("start")
                .parallelGateway("fork")
                .callActivity("call").zeebeProcessId("child")
                .userTask("multi").multiInstance().parallel().zeebeInputCollectionExpression("=items").multiInstanceDone()
                .endEvent("end")
                .done();

        Map<String, List<String>> unsupported = UnsupportedElementHandler.findUnsupported(model);

        assertEquals(List.of("fork"), unsupported.get("parallelGateway"));
        assertEquals(List.of("call"), unsupported.get("callActivity"));
        assertEquals(List.of("multi"), unsupported.get(UnsupportedElementHandler.MULTI_INSTANCE));
    }

    @Test
    @DisplayName("Parsing Model 08 logs one WARNING and adds no inconsistency")
    void warningLoggedWithoutInconsistency() throws IOException {
        Logger logger = Logger.getLogger(UnsupportedElementHandler.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler capture = new Handler() {
            @Override public void publish(LogRecord r) { records.add(r); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(capture);
        try (InputStream model = getClass().getResourceAsStream("/models/model_08.bpmn");
             InputStream config = getClass().getResourceAsStream("/config/test_config_03.yaml")) {

            var workflow = ModelParser.parser(model, ConfigLoader.loadConfig(config));

            assertEquals(0, workflow.inconsistenciesSize());
            assertEquals(1, records.size());
            assertEquals(Level.WARNING, records.get(0).getLevel());
            assertTrue(records.get(0).getMessage().contains("boundaryEvent"));
        } finally {
            logger.removeHandler(capture);
        }
    }
}
