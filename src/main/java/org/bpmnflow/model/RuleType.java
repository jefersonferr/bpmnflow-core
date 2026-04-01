package org.bpmnflow.model;

/**
 * Tipo semântico de uma regra de workflow extraída do modelo BPMN.
 * Cada valor representa um padrão de conectividade entre elementos do diagrama.
 */
public enum RuleType {

    /** StartEvent → Task: regra de entrada do fluxo. */
    START_TO_TASK(1),

    /** Task → Task: transição direta entre atividades. */
    TASK_TO_TASK(2),

    /** Task → Merge → EndEvent: encerramento via gateway de merge. */
    TASK_TO_MERGE_TO_END(3),

    /** Task → Merge → Task: continuação do fluxo via gateway de merge. */
    TASK_TO_MERGE_TO_TASK(4),

    /** Split → Task: ramificação de fluxo via gateway exclusivo. */
    SPLIT_TO_TASK(5),

    /** Split → Merge: conexão direta entre gateways de split e merge. */
    SPLIT_TO_MERGE(6),

    /** Task → EndEvent: encerramento direto do fluxo. */
    TASK_TO_END(7),

    /** Task → Split → EndEvent: encerramento via gateway de split. */
    TASK_TO_SPLIT_TO_END(8);

    private final int code;

    RuleType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
