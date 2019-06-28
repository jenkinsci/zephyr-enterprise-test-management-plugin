package com.thed.model;

/**
 * Created by prashant on 28/6/19.
 */
public class TCRCatalogTreeTestcase extends BaseEntity {

    private Long tcrCatalogTreeId;
    private Testcase testcase;

    public Long getTcrCatalogTreeId() {
        return tcrCatalogTreeId;
    }

    public void setTcrCatalogTreeId(Long tcrCatalogTreeId) {
        this.tcrCatalogTreeId = tcrCatalogTreeId;
    }

    public Testcase getTestcase() {
        return testcase;
    }

    public void setTestcase(Testcase testcase) {
        this.testcase = testcase;
    }
}
