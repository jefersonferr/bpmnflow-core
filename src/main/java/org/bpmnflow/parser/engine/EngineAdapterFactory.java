package org.bpmnflow.parser.engine;

import org.bpmnflow.parser.BpmnConfigException;
import org.bpmnflow.parser.BpmnPropertiesConfig;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Instantiates the correct {@link EngineAdapter} based on the {@code engine}
 * field in {@link BpmnPropertiesConfig}.
 *
 * <p>This is the only place in the framework that knows the concrete
 * implementations of {@link EngineAdapter}. All other code works with
 * the interface.</p>
 *
 * <p>To add support for a new engine, implement {@link EngineAdapter} and
 * add a {@code case} to the switch below — no other class needs to change.</p>
 */
public final class EngineAdapterFactory {

    private static final Logger LOGGER =
            Logger.getLogger(EngineAdapterFactory.class.getName());

    private EngineAdapterFactory() {}

    public static EngineAdapter create(BpmnPropertiesConfig config) {
        String engine = config.getEngine();
        EngineAdapter adapter = switch (engine) {
            case "camunda7" -> new Camunda7EngineAdapter();
            case "camunda8" -> new Camunda8EngineAdapter();
            default -> throw new BpmnConfigException(
                    "Unsupported engine: '" + engine + "'. " +
                            "Valid values: " + supportedEngines());
        };
        LOGGER.info(() -> "BpmnFlow — active engine adapter: " + adapter.engineId());
        return adapter;
    }

    public static Set<String> supportedEngines() {
        return Set.of("camunda7", "camunda8");
    }
}