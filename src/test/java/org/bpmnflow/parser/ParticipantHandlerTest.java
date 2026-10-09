package org.bpmnflow.parser;

import org.bpmnflow.model.Stage;
import org.bpmnflow.model.Workflow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Model 11: multiple participants (two pools with a process + one black-box pool)")
class ParticipantHandlerTest {

    private final Logger logger = Logger.getLogger(ParticipantHandler.class.getName());
    private final List<LogRecord> records = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override public void publish(LogRecord r) { records.add(r); }
        @Override public void flush() { }
        @Override public void close() { }
    };

    private Workflow workflow;

    @BeforeAll
    void parseModel() throws Exception {
        logger.addHandler(capture);
        try (InputStream model = getClass().getResourceAsStream("/models/model_11.bpmn");
             InputStream config = getClass().getResourceAsStream("/config/test_config_02.yaml")) {
            workflow = ModelParser.parser(model, ConfigLoader.loadConfig(config));
        }
    }

    @AfterAll
    void detachHandler() {
        logger.removeHandler(capture);
    }

    @Test
    @DisplayName("Header comes from the first pool with a process — not overwritten by later pools")
    void headerFromFirstProcessPool() {
        assertAll(
                () -> assertEquals("Main Process", workflow.getName()),
                () -> assertEquals("Process_Main", workflow.getId()),
                () -> assertEquals("1.0", workflow.getVersion()),
                () -> assertEquals("Main process documentation", workflow.getDocumentation()),
                () -> assertEquals("MT", workflow.getType()),
                () -> assertEquals("MST", workflow.getSubtype())
        );
    }

    @Test
    @DisplayName("Stages come only from the main process")
    void stagesFromMainProcessOnly() {
        assertEquals(List.of("MN"), workflow.getStages().stream().map(Stage::getCode).toList());
    }

    @Test
    @DisplayName("Black-box pool produces no inconsistency")
    void blackBoxPoolIgnored() {
        assertEquals(0, workflow.inconsistenciesSize(), () -> workflow.getInconsistencies().toString());
    }

    @Test
    @DisplayName("One WARNING names the ignored pool with a process")
    void warningLogged() {
        assertEquals(1, records.size());
        LogRecord record = records.get(0);
        assertEquals(Level.WARNING, record.getLevel());
        assertTrue(record.getMessage().contains("Participant_Main"));
        assertTrue(record.getMessage().contains("Participant_Secondary"));
        assertFalse(record.getMessage().contains("Participant_Customer"));
    }
}
