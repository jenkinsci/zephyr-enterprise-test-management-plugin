package com.thed.model;

import java.util.Map;

public class Testcase extends BaseEntity {

    private String name;
    private String tag;
    private String description;
    private Long projectId;
    private Long releaseId;
    private Boolean automated;
    private Boolean fromJenkins=true;
    private String scriptName;
    private Long testcaseId;
    private TestStep testSteps;
    private Map<String, Object> customProperties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }

    public Boolean getAutomated() {
        return automated;
    }

    public void setAutomated(Boolean automated) {
        this.automated = automated;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public Long getTestcaseId() {
        return testcaseId;
    }

    public void setTestcaseId(Long testcaseId) {
        this.testcaseId = testcaseId;
    }

    public TestStep getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(TestStep testSteps) {
        this.testSteps = testSteps;
    }

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }

    public Boolean getFromJenkins() {
        return fromJenkins;
    }

    public void setFromJenkins(Boolean fromJenkins) {
        this.fromJenkins = fromJenkins;
    }
}
