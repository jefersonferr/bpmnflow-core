package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Node {

    String name;
    String documentation;

    public Node(String name, String documentation) {
        this.name = name;
        this.documentation = documentation;
    }

}
