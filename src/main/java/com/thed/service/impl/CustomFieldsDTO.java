package com.thed.service.impl;

public class CustomFieldsDTO {

    private Long id;
    private String entityName;
    private Boolean systemField;
    private Long fieldTypeMetadata;
    private String fieldName;
    private String displayName;
    private Boolean mandatory;
    private Boolean importable;
    private Boolean isVisible;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Boolean getSystemField() {
        return systemField;
    }

    public void setSystemField(Boolean systemField) {
        this.systemField = systemField;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Long getFieldTypeMetadata() {
        return fieldTypeMetadata;
    }

    public void setFieldTypeMetadata(Long fieldTypeMetadata) {
        this.fieldTypeMetadata = fieldTypeMetadata;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getImportable() {
        return importable;
    }

    public void setImportable(Boolean importable) {
        this.importable = importable;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Boolean getVisible() {
        return isVisible;
    }

    public void setVisible(Boolean visible) {
        isVisible = visible;
    }
}
