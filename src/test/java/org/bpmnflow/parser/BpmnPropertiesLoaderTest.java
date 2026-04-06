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
        // Config com task.stage required=true
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

    // --- propriedade existe e required=true ---

    @Test
    @DisplayName("Propriedade existente e required=true deve retornar isRequired() true")
    void givenExistingRequiredProperty_whenGet_thenIsRequiredTrue() {
        ModelProperty prop = loaderWithConfig.getTask("stage");
        assertTrue(prop.isRequired());
    }

    // --- propriedade existe e required=false ---

    @Test
    @DisplayName("Propriedade existente e required=false deve retornar isRequired() false")
    void givenExistingOptionalProperty_whenGet_thenIsRequiredFalse() {
        ModelProperty prop = loaderWithConfig.getTask("activity");
        assertFalse(prop.isRequired());
    }

    // --- propriedade inexistente dentro de um tipo mapeado ---

    @Test
    @DisplayName("Propriedade ausente no config deve retornar ABSENT sem NPE")
    void givenMissingProperty_whenGet_thenReturnsAbsentWithoutNPE() {
        // "documentation" não está no config de task
        ModelProperty prop = loaderWithConfig.getTask("documentation");

        assertNotNull(prop, "Nunca deve retornar null");
        assertFalse(prop.isRequired(), "ABSENT deve ter required=false");
        assertFalse(prop.isExtension(), "ABSENT deve ter extension=false");
        assertEquals("<absent>", prop.getName());
    }

    // --- tipo de elemento inteiramente ausente do config ---

    @Test
    @DisplayName("Tipo de elemento ausente do config deve retornar ABSENT sem NPE")
    void givenMissingElementType_whenGet_thenReturnsAbsentWithoutNPE() {
        // "lane" nem existe no config deste loader
        ModelProperty prop = loaderWithConfig.getLane("stage");

        assertNotNull(prop, "Nunca deve retornar null");
        assertFalse(prop.isRequired());
        // Garantia de que .isRequired() não explode — este era o bug
        assertDoesNotThrow(prop::isRequired);
    }

    // --- config completamente vazio ---

    @Test
    @DisplayName("Config vazio deve retornar ABSENT para qualquer chamada sem NPE")
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

    // --- ABSENT é singleton seguro ---

    @Test
    @DisplayName("Duas chamadas para propriedades ausentes devem retornar a mesma instância ABSENT")
    void givenTwoMissingProperties_whenGet_thenSameAbsentInstance() {
        ModelProperty a = loaderWithConfig.getTask("inexistente_a");
        ModelProperty b = loaderWithConfig.getLane("inexistente_b");
        assertSame(ModelProperty.ABSENT, a);
        assertSame(ModelProperty.ABSENT, b);
    }
}
