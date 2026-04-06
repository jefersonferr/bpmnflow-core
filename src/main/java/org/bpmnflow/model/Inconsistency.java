package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Inconsistency {

    Integer type;
    String description;

    public Inconsistency(Integer type, String description) {
        this.type = type;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Inconsistency{" +
                "type=" + type +
                ", description='" + description + '\'' +
                '}';
    }

}
