package org.bpmnflow.model;

import lombok.Getter;

/**
 * Specialisation of {@link ActivityNode} for ServiceTask elements
 * that declare an API call via a connector extension.
 *
 * Created by FlowNodeHandler when the active EngineAdapter returns
 * a non-null ApiHandlerDefinition for a ServiceTask. Plain Task elements
 * continue to produce plain ActivityNode instances.
 *
 * Usage by the execution engine:
 *
 *   if (node instanceof ApiActivityNode apiNode) {
 *       ApiHandlerDefinition def = apiNode.getApiHandler();
 *       apiHandlerProvider.execute(def, instanceVariables);
 *   }
 */
@Getter
public class ApiActivityNode extends ActivityNode {

    /**
     * Full API-call definition extracted from the connector extension.
     * Never null — the parser only creates this subtype when
     * extractApiHandler() returns a non-null result.
     */
    private final ApiHandlerDefinition apiHandler;

    public ApiActivityNode(String stageCode,
                           String activityCode,
                           String name,
                           String documentation,
                           ApiHandlerDefinition apiHandler) {
        super(stageCode, activityCode, name, documentation);
        this.apiHandler = apiHandler;
    }

    @Override
    public String toString() {
        return "ApiActivityNode{" +
                "abbreviation='" + getAbbreviation() + '\'' +
                ", name='" + getName() + '\'' +
                ", apiHandler=" + apiHandler +
                ", conclusions=" + getConclusions() +
                '}';
    }
}