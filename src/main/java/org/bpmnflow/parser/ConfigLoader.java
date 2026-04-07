package org.bpmnflow.parser;

import org.bpmnflow.parser.engine.EngineAdapterFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads and parses the BPMN configuration from a YAML source.
 *
 * <p>Both overloads throw {@link BpmnConfigException} on any failure —
 * an invalid or missing config must never silently produce an empty
 * configuration, as that would cause the parser to skip all validations
 * without any indication that something went wrong.</p>
 */
public class ConfigLoader {

    private static final Set<String> SUPPORTED_ENGINES =
            EngineAdapterFactory.supportedEngines();

    private ConfigLoader() {}

    /**
     * Loads the config from a filesystem path.
     * Retained for backward compatibility with the existing public API.
     */
    public static BpmnPropertiesConfig loadConfig(String externalConfigPath) {
        try (InputStream input = Files.newInputStream(Path.of(externalConfigPath))) {
            return loadConfig(input);
        } catch (IOException e) {
            throw new BpmnConfigException(
                    "Failed to read BPMN config file: " + externalConfigPath, e);
        }
    }

    /**
     * Loads the config from an {@link InputStream}.
     * Preferred when the config comes from the classpath (tests, Spring Boot, embedded JARs).
     */
    public static BpmnPropertiesConfig loadConfig(InputStream configStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> rawConfig = yaml.load(configStream);
            return parseRawConfig(rawConfig);
        } catch (BpmnConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new BpmnConfigException("Failed to parse BPMN config stream", e);
        }
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static BpmnPropertiesConfig parseRawConfig(Map<String, Object> rawConfig) {
        if (rawConfig == null || !rawConfig.containsKey("bpmn_model_parser")) {
            throw new BpmnConfigException(
                    "Invalid config: root key 'bpmn_model_parser' not found.");
        }

        Map<String, Object> bpmnModelParser =
                (Map<String, Object>) rawConfig.get("bpmn_model_parser");

        BpmnPropertiesConfig config = new BpmnPropertiesConfig();

        // Read the "engine" field.
        // If absent → keep default "camunda7" (backward compatible).
        // If present → validate that the value is supported.
        if (bpmnModelParser.containsKey("engine")) {
            String engine = String.valueOf(bpmnModelParser.get("engine")).trim();
            if (!SUPPORTED_ENGINES.contains(engine)) {
                throw new BpmnConfigException(
                        "Invalid value for 'engine': '" + engine + "'. " +
                        "Valid values: " + SUPPORTED_ENGINES);
            }
            config.setEngine(engine);
        }

        if (!bpmnModelParser.containsKey("model_properties")) {
            throw new BpmnConfigException(
                    "Invalid config: key 'model_properties' not found " +
                    "under 'bpmn_model_parser'.");
        }

        config.setExtensionProperties(extractExtensionProperties(bpmnModelParser));
        return config;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<ModelProperty>> extractExtensionProperties(
            Map<String, Object> bpmnModelParser) {

        Map<String, Object> rawProperties =
                (Map<String, Object>) bpmnModelParser.get("model_properties");
        Map<String, List<ModelProperty>> extensionProperties = new HashMap<>();

        for (Map.Entry<String, Object> entry : rawProperties.entrySet()) {
            String key = entry.getKey();
            List<Object> rawList = (List<Object>) entry.getValue();
            List<ModelProperty> properties = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    properties.add(mapToModelProperty((Map<String, Object>) obj));
                }
            }
            extensionProperties.put(key, properties);
        }
        return extensionProperties;
    }

    private static ModelProperty mapToModelProperty(Map<String, Object> propertyMap) {
        ModelProperty property = new ModelProperty();
        property.setName((String) propertyMap.get("name"));
        property.setRequired(Boolean.TRUE.equals(propertyMap.get("required")));
        property.setExtension(Boolean.TRUE.equals(propertyMap.get("extension")));
        return property;
    }
}
