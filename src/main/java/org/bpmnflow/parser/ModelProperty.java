package org.bpmnflow.parser;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ModelProperty {

    /**
     * Instância sentinela que representa uma propriedade ausente no config.
     * Retorna required=false e extension=false — comportamento neutro e seguro.
     * Elimina a necessidade de null-checks nos 20 call sites de isRequired().
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