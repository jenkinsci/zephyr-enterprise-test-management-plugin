package com.thed.model;

import java.io.Serializable;

/**
 * Created by prashant on 21/6/19.
 */
public class Project extends BaseEntity {

    private String name;
    private Boolean globalProject;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "{id: " + getId() + ", name: " + name + "}";
    }

    public Boolean getGlobalProject() {
        return globalProject;
    }

    public void setGlobalProject(Boolean globalProject) {
        this.globalProject = globalProject;
    }
}
