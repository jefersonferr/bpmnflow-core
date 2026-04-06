package org.bpmnflow.parser.engine;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;

import java.util.Map;

/**
 * SPI (Service Provider Interface) que abstrai o acesso a elementos
 * engine-específicos de um arquivo BPMN.
 *
 * <p>Cada engine (Camunda 7, Camunda 8) armazena extension properties
 * e o version tag em namespaces e estruturas XML diferentes.
 * Esta interface isola essas diferenças do restante do framework,
 * que permanece agnóstico ao engine.</p>
 *
 * <p>Implementações são criadas por {@link EngineAdapterFactory}
 * com base no campo {@code engine} do {@code config.yaml}.
 * Nenhum código fora deste pacote deve depender de uma implementação
 * concreta — use sempre o tipo {@link EngineAdapter}.</p>
 *
 * <h2>Convenção de namespaces</h2>
 * <ul>
 *   <li>Camunda 7: {@code http://camunda.org/schema/1.0/bpmn} (prefixo {@code camunda:})</li>
 *   <li>Camunda 8: {@code http://camunda.org/schema/zeebe/1.0} (prefixo {@code zeebe:})</li>
 * </ul>
 */
public interface EngineAdapter {

    /**
     * Extrai todas as extension properties do elemento BPMN informado.
     *
     * @param element qualquer elemento BPMN que possa ter extensionElements
     * @return mapa mutável nome→valor; nunca null, pode ser vazio
     */
    Map<String, String> extractProperties(BaseElement element);

    /**
     * Extrai o version tag do elemento {@code <process>}.
     *
     * <ul>
     *   <li>C7: atributo {@code camunda:versionTag} no próprio elemento {@code <process>}</li>
     *   <li>C8: elemento filho {@code <zeebe:versionTag value="...">} dentro de extensionElements</li>
     * </ul>
     *
     * @param process o elemento Process do modelo BPMN
     * @return o valor do version tag, ou {@code null} se ausente
     */
    String extractVersionTag(BaseElement process);

    /**
     * Identificador do engine. Valores: {@code "camunda7"}, {@code "camunda8"}.
     */
    String engineId();
}
