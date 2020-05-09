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
     * Get releaseTestSchedules for given cyclePhaseId.
     * @param cyclePhaseId
     * @param offset Offset for starting results
     * @param pageSize Number of results per page, 0 will return all
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId, Integer offset, Integer pageSize) throws URISyntaxException;

    /**
     * Execute given releaseTestSchedule ids for given statusId.
     * @param rtsIds
     * @param statusId
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> executeReleaseTestSchedules(Set<Long> rtsIds, String statusId) throws URISyntaxException;

    /**
     * Add testStep results.
     * @param testStepResults
     * @return
     * @throws URISyntaxException
     */
    List<TestStepResult> addTestStepResults(List<TestStepResult> testStepResults) throws URISyntaxException;

}
