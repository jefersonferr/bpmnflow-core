package org.bpmnflow.parser.engine;

import org.bpmnflow.parser.BpmnConfigException;
import org.bpmnflow.parser.BpmnPropertiesConfig;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Instancia o {@link EngineAdapter} correto com base no campo
 * {@code engine} do {@link BpmnPropertiesConfig}.
 *
 * <p>É o único ponto do framework que conhece as implementações concretas
 * de {@link EngineAdapter}. Todo o restante do código trabalha com a interface.</p>
 *
 * <p>Para adicionar suporte a um novo engine, basta implementar {@link EngineAdapter}
 * e adicionar um {@code case} no switch abaixo — nenhuma outra classe precisa mudar.</p>
 */
public final class EngineAdapterFactory {

    private static final Logger LOGGER =
            Logger.getLogger(EngineAdapterFactory.class.getName());

    private EngineAdapterFactory() {}

    public static EngineAdapter create(BpmnPropertiesConfig config) {
        String engine = config.getEngine();
        LOGGER.info(() -> "BpmnFlow — engine adapter ativo: " + engine);
        return switch (engine) {
            case "camunda7" -> new Camunda7EngineAdapter();
            case "camunda8" -> new Camunda8EngineAdapter();
            default -> throw new BpmnConfigException(
                    "Engine não suportado: '" + engine + "'. " +
                    "Valores válidos: " + supportedEngines());
        };
    }

    public static Set<String> supportedEngines() {
        return Set.of("camunda7", "camunda8");
    }
}
