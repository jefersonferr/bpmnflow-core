package org.bpmnflow.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelParser: unreadable models raise BpmnModelException")
class ModelParserExceptionTest {

    private static BpmnPropertiesConfig config() throws Exception {
        try (InputStream in = ModelParserExceptionTest.class
                .getResourceAsStream("/config/test_config_03.yaml")) {
            return ConfigLoader.loadConfig(in);
        }
    }

    @Test
    @DisplayName("Malformed XML → BpmnModelException with the original cause")
    void malformedXml() throws Exception {
        InputStream broken = new ByteArrayInputStream(
                "<bpmn:definitions><not-closed>".getBytes(StandardCharsets.UTF_8));
        BpmnPropertiesConfig cfg = config();

        BpmnModelException ex = assertThrows(BpmnModelException.class,
                () -> ModelParser.parser(broken, cfg));
        assertNotNull(ex.getCause());
        assertTrue(ex.getMessage().startsWith("Failed to parse BPMN model stream"));
    }

    @Test
    @DisplayName("Null stream → BpmnModelException")
    void nullStream() throws Exception {
        BpmnPropertiesConfig cfg = config();
        assertThrows(BpmnModelException.class, () -> ModelParser.parser(null, cfg));
    }

    @Test
    @DisplayName("BpmnModelException is still a RuntimeException (backward compatible)")
    void isRuntimeException() {
        assertInstanceOf(RuntimeException.class, new BpmnModelException("x"));
    }
}
