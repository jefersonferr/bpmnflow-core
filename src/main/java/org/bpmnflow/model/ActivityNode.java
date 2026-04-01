package org.bpmnflow.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ActivityNode extends Node {

    String stageCode;
    String activityCode;
    String abbreviation;
    List<Conclusion> conclusions = new ArrayList<>();

    public ActivityNode(String stageCode, String activityCode, String name, String documentation) {
        super(name, documentation);
        this.stageCode = stageCode;
        this.activityCode = activityCode;
        this.abbreviation = stageCode + "-" + activityCode;
    }

    @Override
    public String toString() {
        return "ActivityNode{" +
                "abbreviation='" + abbreviation + "'" +
                ", name='" + name + "'" +
                ", conclusions=" + formatConclusions() +
                "}";
    }

    private String formatConclusions() {
        if (conclusions.isEmpty()) return "[]";
        return "[" + conclusions.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")) + "]";
    }

    public void addConclusion(Conclusion conclusion) { conclusions.add(conclusion); }
}
