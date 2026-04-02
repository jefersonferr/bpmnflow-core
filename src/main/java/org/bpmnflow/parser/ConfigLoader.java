package org.bpmnflow.parser;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads and parses the BPMN properties configuration from a YAML source.
 *
 * <p>Both overloads throw {@link BpmnConfigException} on any failure —
 * an invalid or missing config must never silently produce an empty
 * configuration, as that would cause the parser to skip all validations
 * without any indication that something went wrong.</p>
 */
public class ConfigLoader {

    private ConfigLoader() {}

    /**
     * Loads the config from a filesystem path.
     * Retained for backward compatibility with the existing public API.
     *
     * @param externalConfigPath absolute or relative path to the YAML file
     * @return a fully populated {@link BpmnPropertiesConfig}
     * @throws BpmnConfigException if the file cannot be read or parsed
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
     * Preferred when the config comes from the classpath, avoiding path
     * issues on Windows with {@code getResource().getPath()}.
     *
     * @param configStream the YAML content as a stream
     * @return a fully populated {@link BpmnPropertiesConfig}
     * @throws BpmnConfigException if the stream cannot be parsed or is structurally invalid
     */
    public static BpmnPropertiesConfig loadConfig(InputStream configStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> rawConfig = yaml.load(configStream);
            Map<String, List<ModelProperty>> extensionProperties = extractExtensionProperties(rawConfig);
            BpmnPropertiesConfig config = new BpmnPropertiesConfig();
            config.setExtensionProperties(extensionProperties);
            return config;
        } catch (BpmnConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new BpmnConfigException(
                    "Failed to parse BPMN config stream", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<ModelProperty>> extractExtensionProperties(Map<String, Object> rawConfig) {
        if (rawConfig == null || !rawConfig.containsKey("bpmn_model_parser")) {
            throw new BpmnConfigException(
                    "Invalid BPMN config: missing root key 'bpmn_model_parser'");
        }

        Map<String, Object> bpmnModelParser = (Map<String, Object>) rawConfig.get("bpmn_model_parser");
        if (!bpmnModelParser.containsKey("model_properties")) {
            throw new BpmnConfigException(
                    "Invalid BPMN config: missing key 'model_properties' under 'bpmn_model_parser'");
        }

        Map<String, Object> rawProperties = (Map<String, Object>) bpmnModelParser.get("model_properties");
        Map<String, List<ModelProperty>> extensionProperties = new HashMap<>();

        for (Map.Entry<String, Object> entry : rawProperties.entrySet()) {
            String key = entry.getKey();
            List<Object> rawList = (List<Object>) entry.getValue();
            List<ModelProperty> properties = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<String, Object> propertyMap = (Map<String, Object>) obj;
                    properties.add(mapToModelProperty(propertyMap));
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