package org.bpmnflow.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class Workflow {

    String name;
    String id;
    String version;
    String documentation;
    String type;
    String subtype;
    List<Stage> stages = new ArrayList<>();
    List<ActivityNode> activities = new ArrayList<>();
    List<Inconsistency> inconsistencies = new ArrayList<>();
    List<WorkflowRule> rules = new ArrayList<>();

    public Workflow(String name, String id, String version, String documentation, String type, String subtype) {
        this.name = name;
        this.id = id;
        this.version = version;
        this.documentation = documentation;
        this.type = type;
        this.subtype = subtype;
    }

    @Override
    public String toString() {
        return "Workflow{\n" +
                "  name='" + name + "'\n" +
                "  id='" + id + "'\n" +
                "  version='" + version + "'\n" +
                "  documentation='" + documentation + "'\n" +
                "  type='" + type + "'\n" +
                "  subtype='" + subtype + "'\n" +
                "  stages=" + formatList(stages, "  ") + "\n" +
                "  activities=" + formatList(activities, "  ") + "\n" +
                "  inconsistencies=" + formatList(inconsistencies, "  ") + "\n" +
                "  rules=" + formatList(rules, "  ") + "\n" +
                "}";
    }

    private static <T> String formatList(List<T> list, String indent) {
        if (list.isEmpty()) return "[]";
        String items = list.stream()
                .map(item -> indent + "  " + item)
                .collect(Collectors.joining(",\n"));
        return "[\n" + items + "\n" + indent + "]";
    }

    public void addStage(Stage stage) { stages.add(stage); }
    public void setStages(List<Stage> stages) { this.stages = stages; }
    public int stagesSize() { return stages.size(); }

    public void addActivity(ActivityNode activityNode) { activities.add(activityNode); }
    public int activitiesSize() { return activities.size(); }

    public void addInconsistency(Inconsistency inconsistency) { inconsistencies.add(inconsistency); }
    public void setInconsistencies(List<Inconsistency> inconsistencies) { this.inconsistencies = inconsistencies; }
    public int inconsistenciesSize() { return inconsistencies.size(); }

    public void addRule(WorkflowRule workflowRule) { rules.add(workflowRule); }
    public int rulesSize() { return rules.size(); }
}