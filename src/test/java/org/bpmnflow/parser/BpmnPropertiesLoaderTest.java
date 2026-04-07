package org.bpmnflow.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BpmnPropertiesLoader")
class BpmnPropertiesLoaderTest {

    private BpmnPropertiesLoader loaderWithConfig;
    private BpmnPropertiesLoader loaderEmptyConfig;

    @BeforeEach
    void setUp() {
        // Config with task.stage required=true
        ModelProperty stageRequired = new ModelProperty();
        stageRequired.setName("stage");
        stageRequired.setRequired(true);
        stageRequired.setExtension(true);

        ModelProperty activityOptional = new ModelProperty();
        activityOptional.setName("activity");
        activityOptional.setRequired(false);
        activityOptional.setExtension(true);

        BpmnPropertiesConfig config = new BpmnPropertiesConfig();
        config.setExtensionProperties(Map.of(
                "task", List.of(stageRequired, activityOptional)
        ));

        loaderWithConfig  = new BpmnPropertiesLoader(config);
        loaderEmptyConfig = new BpmnPropertiesLoader(new BpmnPropertiesConfig());
    }

    // --- property exists and required=true ---

    @Test
    @DisplayName("Existing property with required=true should return isRequired() true")
    void givenExistingRequiredProperty_whenGet_thenIsRequiredTrue() {
        ModelProperty prop = loaderWithConfig.getTask("stage");
        assertTrue(prop.isRequired());
    }

    // --- property exists and required=false ---

    @Test
    @DisplayName("Existing property with required=false should return isRequired() false")
    void givenExistingOptionalProperty_whenGet_thenIsRequiredFalse() {
        ModelProperty prop = loaderWithConfig.getTask("activity");
        assertFalse(prop.isRequired());
    }

    // --- property absent within a mapped type ---

    @Test
    @DisplayName("Property absent from config should return ABSENT without NPE")
    void givenMissingProperty_whenGet_thenReturnsAbsentWithoutNPE() {
        // "documentation" is not in the task config
        ModelProperty prop = loaderWithConfig.getTask("documentation");

        assertNotNull(prop, "Must never return null");
        assertFalse(prop.isRequired(), "ABSENT must have required=false");
        assertFalse(prop.isExtension(), "ABSENT must have extension=false");
        assertEquals("<absent>", prop.getName());
    }

    // --- element type entirely absent from config ---

    @Test
    @DisplayName("Element type absent from config should return ABSENT without NPE")
    void givenMissingElementType_whenGet_thenReturnsAbsentWithoutNPE() {
        // "lane" does not exist in this loader's config
        ModelProperty prop = loaderWithConfig.getLane("stage");

        assertNotNull(prop, "Must never return null");
        assertFalse(prop.isRequired());
        // Guarantee that .isRequired() does not throw — this was the original bug
        assertDoesNotThrow(prop::isRequired);
    }

    // --- completely empty config ---

    @Test
    @DisplayName("Empty config should return ABSENT for any call without NPE")
    void givenEmptyConfig_whenAnyGet_thenReturnsAbsentWithoutNPE() {
        assertAll(
                () -> assertDoesNotThrow(() -> loaderEmptyConfig.getTask("stage").isRequired()),
                () -> assertDoesNotThrow(() -> loaderEmptyConfig.getParticipant("name").isRequired()),
                () -> assertDoesNotThrow(() -> loaderEmptyConfig.getProcess("id").isRequired()),
                () -> assertDoesNotThrow(() -> loaderEmptyConfig.getLane("stage").isRequired()),
                () -> assertDoesNotThrow(() -> loaderEmptyConfig.getStartEvent("process_status").isRequired()),
                () -> assertDoesNotThrow(() -> loaderEmptyConfig.getEndEvent("process_status").isRequired()),
                () -> assertDoesNotThrow(() -> loaderEmptyConfig.getSequenceFlow("conclusion").isRequired())
        );
    }

    // --- ABSENT is a safe singleton ---

    @Test
    @DisplayName("Two calls for absent properties should return the same ABSENT instance")
    void givenTwoMissingProperties_whenGet_thenSameAbsentInstance() {
        ModelProperty a = loaderWithConfig.getTask("missing_a");
        ModelProperty b = loaderWithConfig.getLane("missing_b");
        assertSame(ModelProperty.ABSENT, a);
        assertSame(ModelProperty.ABSENT, b);
    }
}
