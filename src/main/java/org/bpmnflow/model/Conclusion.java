package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Conclusion {
    String code;
    String name;
    String documentation;

    public Conclusion(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Conclusion{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
