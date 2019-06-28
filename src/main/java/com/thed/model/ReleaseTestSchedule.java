package com.thed.model;

/**
 * Created by prashant on 29/6/19.
 */
public class ReleaseTestSchedule extends BaseEntity {

    private String status;
    private Long testerId;
    private TCRCatalogTreeTestcase tcrTreeTestcase;
    private Long cyclePhaseId;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTesterId() {
        return testerId;
    }

    public void setTesterId(Long testerId) {
        this.testerId = testerId;
    }

    public TCRCatalogTreeTestcase getTcrTreeTestcase() {
        return tcrTreeTestcase;
    }

    public void setTcrTreeTestcase(TCRCatalogTreeTestcase tcrTreeTestcase) {
        this.tcrTreeTestcase = tcrTreeTestcase;
    }

    public Long getCyclePhaseId() {
        return cyclePhaseId;
    }

    public void setCyclePhaseId(Long cyclePhaseId) {
        this.cyclePhaseId = cyclePhaseId;
    }
}
