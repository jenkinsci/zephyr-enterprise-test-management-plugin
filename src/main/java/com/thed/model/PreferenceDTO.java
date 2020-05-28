package com.thed.model;

public class PreferenceDTO {

    private String name;
    private String value;
    private String defaultValue;
    private Boolean isCustomizable;
    private Boolean editable;
    private Integer accessLevel;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getIsCustomizable() {
        return isCustomizable;
    }

    public void setIsCustomizable(Boolean isCustomizable) {
        this.isCustomizable = isCustomizable;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Integer getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(Integer accessLevel) {
        this.accessLevel = accessLevel;
    }

}