package com.thed.model;

/**
 * Created by prashant on 10/1/20.
 */
public class TestStepResult extends BaseEntity {

    private Long cyclePhaseId;
    private Long releaseTestScheduleId;
    private Long status;
    private Long testStepId;

    public Long getCyclePhaseId() {
        return cyclePhaseId;
    }

    public void setCyclePhaseId(Long cyclePhaseId) {
        this.cyclePhaseId = cyclePhaseId;
    }

    public Long getReleaseTestScheduleId() {
        return releaseTestScheduleId;
    }

    public void setReleaseTestScheduleId(Long releaseTestScheduleId) {
        this.releaseTestScheduleId = releaseTestScheduleId;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public Long getTestStepId() {
        return testStepId;
    }

    public void setTestStepId(Long testStepId) {
        this.testStepId = testStepId;
    }
}
