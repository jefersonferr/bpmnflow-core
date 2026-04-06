package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkflowRule {

    RuleType type;
    ActivityNode source;
    ActivityNode target;
    Conclusion conclusion;
    String processStatus;

    public WorkflowRule(RuleType type, ActivityNode source, ActivityNode target, Conclusion conclusion, String processStatus) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.conclusion = conclusion;
        this.processStatus = processStatus;
    }

    @Override
    public String toString() {
        return "WorkflowRule{" +
                "type='" + type + '\'' +
                ", source='" + ((source == null) ? "null" : source.getAbbreviation()) + '\'' +
                ", target='" + ((target == null) ? "null" : target.getAbbreviation()) + '\'' +
                ", conclusion='" + ((conclusion == null) ? "null" : conclusion.getCode()) + '\'' +
                ", processStatus='" + processStatus + '\'' +
                '}';
    }
}
