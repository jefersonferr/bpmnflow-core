package org.bpmnflow.parser;

import org.bpmnflow.parser.engine.EngineAdapterFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Carrega e parseia a configuração BPMN a partir de uma fonte YAML.
 *
 * <p>Ambas as sobrecargas lançam {@link BpmnConfigException} em qualquer falha —
 * uma config inválida ou ausente nunca deve produzir silenciosamente uma
 * configuração vazia, pois isso faria o parser ignorar todas as validações
 * sem nenhum aviso.</p>
 */
public class ConfigLoader {

    private static final Set<String> SUPPORTED_ENGINES =
            EngineAdapterFactory.supportedEngines();

    private ConfigLoader() {}

    /**
     * Carrega o config de um caminho no filesystem.
     * Mantido por backward compatibility com a API pública existente.
     */
    public static BpmnPropertiesConfig loadConfig(String externalConfigPath) {
        try (InputStream input = Files.newInputStream(Path.of(externalConfigPath))) {
            return loadConfig(input);
        } catch (IOException e) {
            throw new BpmnConfigException(
                    "Falha ao ler o arquivo de config BPMN: " + externalConfigPath, e);
        }
    }

    /**
     * Carrega o config de um {@link InputStream}.
     * Preferível quando o config vem do classpath (testes, Spring Boot, JARs embutidos).
     */
    public static BpmnPropertiesConfig loadConfig(InputStream configStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> rawConfig = yaml.load(configStream);
            return parseRawConfig(rawConfig);
        } catch (BpmnConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new BpmnConfigException("Falha ao parsear o stream de config BPMN", e);
        }
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static BpmnPropertiesConfig parseRawConfig(Map<String, Object> rawConfig) {
        if (rawConfig == null || !rawConfig.containsKey("bpmn_model_parser")) {
            throw new BpmnConfigException(
                    "Config inválido: chave raiz 'bpmn_model_parser' não encontrada.");
        }

        Map<String, Object> bpmnModelParser =
                (Map<String, Object>) rawConfig.get("bpmn_model_parser");

        BpmnPropertiesConfig config = new BpmnPropertiesConfig();

        // Leitura do campo "engine"
        // Se ausente → mantém default "camunda7" (backward compatible)
        // Se presente → valida que o valor é suportado
        if (bpmnModelParser.containsKey("engine")) {
            String engine = String.valueOf(bpmnModelParser.get("engine")).trim();
            if (!SUPPORTED_ENGINES.contains(engine)) {
                throw new BpmnConfigException(
                        "Valor inválido para 'engine': '" + engine + "'. " +
                        "Valores válidos: " + SUPPORTED_ENGINES);
            }
            config.setEngine(engine);
        }

        if (!bpmnModelParser.containsKey("model_properties")) {
            throw new BpmnConfigException(
                    "Config inválido: chave 'model_properties' não encontrada " +
                    "dentro de 'bpmn_model_parser'.");
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
