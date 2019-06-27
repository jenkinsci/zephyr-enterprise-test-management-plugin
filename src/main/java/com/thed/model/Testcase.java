package com.thed.model;

public class Testcase extends BaseEntity {

    private String name;
    private Long projectId;
    private Long releaseId;
    private Long tcrCatalogTreeId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Long getTcrCatalogTreeId() {
        return tcrCatalogTreeId;
    }

    public void setTcrCatalogTreeId(Long tcrCatalogTreeId) {
        this.tcrCatalogTreeId = tcrCatalogTreeId;
    }
}