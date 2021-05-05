package com.thed.model;

import java.util.Date;

public class ExecutionRequest {

    private Long rtsId;
    private Long testerId;
    private String status;
    private Date changedOn;
    private Date executedOn;
    private Long actualTime;
    private String notes;
    private Long changeBy;

    public Long getRtsId() {
        return rtsId;
    }

    public void setRtsId(Long rtsId) {
        this.rtsId = rtsId;
    }

    public Long getTesterId() {
        return testerId;
    }

    public void setTesterId(Long testerId) {
        this.testerId = testerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getChangedOn() {
        return changedOn;
    }

    public void setChangedOn(Date changedOn) {
        this.changedOn = changedOn;
    }

    public Date getExecutedOn() {
        return executedOn;
    }

    public void setExecutedOn(Date executedOn) {
        this.executedOn = executedOn;
    }

    public Long getActualTime() {
        return actualTime;
    }

    public void setActualTime(Long actualTime) {
        this.actualTime = actualTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getChangeBy() {
        return changeBy;
    }

    public void setChangeBy(Long changeBy) {
        this.changeBy = changeBy;
    }
}
