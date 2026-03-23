package com.thed.model;

public class TctTestcaseVersionParam {

    private Long tctId;

    private Long testcaseVersionId;

    public TctTestcaseVersionParam() {}

    public TctTestcaseVersionParam(Long tctId, Long testcaseVersionId) {
        setTctId(tctId);
        setTestcaseVersionId(testcaseVersionId);
    }

    public TctTestcaseVersionParam(Long tcrCatalogTreeId, Long id, Long testcaseId) {
        setTctId(tcrCatalogTreeId);
        setTctId(testcaseId);
        setTestcaseVersionId(id);
    }

    public Long getTctId() {
        return tctId;
    }

    public void setTctId(Long tctId) {
        this.tctId = tctId;
    }

    public Long getTestcaseVersionId() {
        return testcaseVersionId;
    }

    public void setTestcaseVersionId(Long testcaseVersionId) {
        this.testcaseVersionId = testcaseVersionId;
    }
}
