package org.bpmnflow.parser;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ModelProperty {

    /**
     * Sentinel instance representing a property absent from the config.
     * Returns required=false and extension=false — a safe, neutral behaviour.
     * Eliminates the need for null-checks at the many call sites of isRequired().
     */
    public static final ModelProperty ABSENT = new ModelProperty() {
        @Override public boolean isRequired()  { return false; }
        @Override public boolean isExtension() { return false; }
        @Override public String  getName()     { return "<absent>"; }
    };

    private String  name;
    private boolean required;
    private boolean extension;
}
