package com.thed.service;

import com.thed.model.Cycle;
import com.thed.model.Project;
import com.thed.model.Release;
import com.thed.model.Testcase;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 20/6/19.
 */
public interface ZephyrRestService {

    /**
     * Verifies given credentials
     * @param hostUrl
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public Boolean verifyCredentials(String hostUrl, String username, String password) throws URISyntaxException;

    /**
     * Verifies given credentials and stores hostAddress if verification succeeds
     * @param hostAddress
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    Boolean login(String hostAddress, String username, String password) throws URISyntaxException;

    /**
     * Get project using project id.
     * @param projectId
     * @return
     * @throws URISyntaxException
     */
    Project getProjectById(Long projectId) throws URISyntaxException;

    /**
     * Create a new cycle with given cycle.
     * @param cycle
     * @return
     */
    Cycle createCycle(Cycle cycle) throws URISyntaxException;

    /**
     * Get all projects for current user.
     * @return
     */
    public List<Project> getAllProjectsForCurrentUser() throws URISyntaxException;

    /**
     * Get all releases for given projectId.
     * @param projectId
     * @return
     * @throws URISyntaxException
     */
    List<Release> getAllReleasesForProjectId(Long projectId) throws URISyntaxException;

    /**
     * Get all cycles for given releaseId.
     * @param releaseId
     * @return
     * @throws URISyntaxException
     */
    List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException;

    /**
     * Create Test Cases
     * @param projectId
     * @param releaseId
     * @param tcrCatalogTreeId
     * @param testNames
     * @return
     * @throws URISyntaxException
     */
    List<Testcase> createTestcases(Long projectId, Long releaseId, Long tcrCatalogTreeId, List<String> testNames) throws URISyntaxException;

    /**
     * Clears all data saved in this instance and related to this.
     */
    void clear();
}
