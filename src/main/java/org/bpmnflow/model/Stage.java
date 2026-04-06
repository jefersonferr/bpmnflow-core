package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Stage {

    String name;
    String code;
    String documentation;

    public Stage(String name, String code) {
        this.name = name;
        this.code = code;
    }

    @Override
    public String toString() {
        return "Stage{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", documentation='" + documentation + '\'' +
                '}';
    }

}
