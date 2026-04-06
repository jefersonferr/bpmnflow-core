package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartEndNode extends Node {
    String processStatus;

    public StartEndNode(String name, String documentation, String processStatus) {
        super(name, documentation);
        this.processStatus = processStatus;
    }

    @Override
    public String toString() {
        return "StartEndNode{" +
                "processStatus='" + processStatus + '\'' +
                ", name='" + name + '\'' +
                ", documentation='" + documentation + '\'' +
                '}';
    }
}
