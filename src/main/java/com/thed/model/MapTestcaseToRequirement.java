package com.thed.model;

import java.util.Set;

/**
 * Created by prashant on 4/1/20.
 */
public class MapTestcaseToRequirement {

    private Set<Long> requirementId;
    private Set<String> requirementAltId;
    private Long testcaseId;
    private Long releaseId;

    public Set<Long> getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Set<Long> requirementId) {
        this.requirementId = requirementId;
    }

    public Set<String> getRequirementAltId() {
        return requirementAltId;
    }

    public void setRequirementAltId(Set<String> requirementAltId) {
        this.requirementAltId = requirementAltId;
    }

    public Long getTestcaseId() {
        return testcaseId;
    }

    public void setTestcaseId(Long testcaseId) {
        this.testcaseId = testcaseId;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }
}
