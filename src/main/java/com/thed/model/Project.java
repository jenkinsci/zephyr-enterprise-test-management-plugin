package com.thed.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by prashant on 21/6/19.
 */
public class Project extends BaseEntity {

    private String name;
    private Boolean globalProject;
    private List<ProjectTeam> members;
    private Date startDate;
    private Date endDate;

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

    public List<ProjectTeam> getMembers() {
        return members;
    }

    public void setMembers(List<ProjectTeam> members) {
        this.members = members;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
