package com.thed.service;

import com.thed.model.ReleaseTestSchedule;
import com.thed.model.TestStepResult;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * Created by prashant on 29/6/19.
 */
public interface ExecutionService extends BaseService {

    /**
     * Get all releaseTestSchedules for given cyclePhaseId.
     * @param cyclePhaseId
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId) throws URISyntaxException;

    /**
     * Execute given releaseTestSchedule ids for either Pass or Fail based on pass.
     * @param rtsIds
     * @param pass
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> executeReleaseTestSchedules(Set<Long> rtsIds, boolean pass) throws URISyntaxException;

    /**
     * Add testStep results.
     * @param testStepResults
     * @return
     * @throws URISyntaxException
     */
    List<TestStepResult> addTestStepResults(List<TestStepResult> testStepResults) throws URISyntaxException;

}
