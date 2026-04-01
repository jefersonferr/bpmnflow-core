package org.bpmnflow.parser;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());

    /**
     * Carrega o config a partir de um path no filesystem.
     * Mantido para compatibilidade com a API pública existente.
     */
    public static BpmnPropertiesConfig loadConfig(String externalConfigPath) {
        try (InputStream input = Files.newInputStream(Path.of(externalConfigPath))) {
            return loadConfig(input);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load BPMN Extension Properties Config from path: " + externalConfigPath, e);
        }
        return new BpmnPropertiesConfig();
    }

    /**
     * Carrega o config a partir de um InputStream.
     * Preferível ao overload com String quando o config vem do classpath,
     * evitando problemas de path no Windows (barra inicial em getResource().getPath()).
     */
    public static BpmnPropertiesConfig loadConfig(InputStream configStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> rawConfig = yaml.load(configStream);
            Map<String, List<ModelProperty>> extensionProperties = extractExtensionProperties(rawConfig);
            BpmnPropertiesConfig config = new BpmnPropertiesConfig();
            config.setExtensionProperties(extensionProperties);
            return config;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load BPMN Extension Properties Config from stream", e);
        }
        return new BpmnPropertiesConfig();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<ModelProperty>> extractExtensionProperties(Map<String, Object> rawConfig) {
        if (rawConfig == null || !rawConfig.containsKey("bpmn_model_parser")) {
            LOGGER.warning("Missing 'bpmn_model_parser' in YAML. Returning empty config.");
            return Collections.emptyMap();
        }

        Map<String, Object> bpmnModelParser = (Map<String, Object>) rawConfig.get("bpmn_model_parser");
        if (!bpmnModelParser.containsKey("model_properties")) {
            LOGGER.warning("Missing 'model_properties' in YAML. Returning empty config.");
            return Collections.emptyMap();
        }

        Map<String, Object> rawExtensionProperties = (Map<String, Object>) bpmnModelParser.get("model_properties");
        Map<String, List<ModelProperty>> extensionProperties = new HashMap<>();

        for (Map.Entry<String, Object> entry : rawExtensionProperties.entrySet()) {
            String key = entry.getKey();
            List<Object> rawList = (List<Object>) entry.getValue();
            List<ModelProperty> properties = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<String, Object> propertyMap = (Map<String, Object>) obj;
                    properties.add(mapToExtensionProperty(propertyMap));
                }
            }
            extensionProperties.put(key, properties);
        }

        return extensionProperties;
    }

    private static ModelProperty mapToExtensionProperty(Map<String, Object> propertyMap) {
        ModelProperty property = new ModelProperty();
        property.setName((String) propertyMap.get("name"));
        property.setRequired(Boolean.TRUE.equals(propertyMap.get("required")));
        property.setExtension(Boolean.TRUE.equals(propertyMap.get("extension")));
        return property;
    }
}
