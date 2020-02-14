package com.thed.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by prashant on 8/1/20.
 */
public class TestStep extends BaseEntity {

    private Long maxId;
    private List<TestStepDetail> steps;
    private Long tcId;
    private Long tctId;

    public TestStep() {
        this.steps = new ArrayList<>();
    }

    public Long getMaxId() {
        return maxId;
    }

    public void setMaxId(Long maxId) {
        this.maxId = maxId;
    }

    public List<TestStepDetail> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStepDetail> steps) {
        this.steps = steps;
    }

    public Long getTcId() {
        return tcId;
    }

    public void setTcId(Long tcId) {
        this.tcId = tcId;
    }

    public Long getTctId() {
        return tctId;
    }

    public void setTctId(Long tctId) {
        this.tctId = tctId;
    }
}
