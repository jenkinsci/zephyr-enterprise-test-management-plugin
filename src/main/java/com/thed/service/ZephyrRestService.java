package com.thed.service;

import com.thed.model.*;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Get all tree nodes such as phases for given releaseId.
     * @param type
     * @param revisionId
     * @param releaseId
     * @return
     * @throws URISyntaxException
     */
    List<TCRCatalogTreeDTO> getTCRCatalogTreeNodes(String type, Long revisionId, Long releaseId) throws URISyntaxException;

    /**
     * Create a tree node such as phase.
     * @param tcrCatalogTreeDTO
     * @return
     * @throws URISyntaxException
     */
    TCRCatalogTreeDTO createTCRCatalogTreeNode(TCRCatalogTreeDTO tcrCatalogTreeDTO) throws URISyntaxException;

    /**
     * Get testcases for given tree id.
     * @param tcrCatalogTreeId
     * @return
     * @throws URISyntaxException
     */
    List<TCRCatalogTreeTestcase> getTestcasesForTreeId(Long tcrCatalogTreeId) throws URISyntaxException;

    /**
     * Create testcases in bulk
     * @param tcrCatalogTreeTestcases
     * @return
     * @throws URISyntaxException
     */
    List<TCRCatalogTreeTestcase> createTestcases(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcases) throws URISyntaxException;

    /**
     * Get cycle for id.
     * @param cycleId
     * @return
     * @throws URISyntaxException
     */
    Cycle getCycleById(Long cycleId) throws URISyntaxException;

    /**
     * Create cycle phase.
     * @param cyclePhase
     * @return
     * @throws URISyntaxException
     */
    CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException;

    /**
     * Adds testcases to free form cycle phase.
     * @param cyclePhase
     * @param treeTestcaseMap
     * @param includeHierarchy
     * @return
     * @throws URISyntaxException
     */
    String addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, Map<Long, Set<Long>> treeTestcaseMap, Boolean includeHierarchy) throws URISyntaxException;

    /**
     * Assigns cycle phase to creator.
     * @param cyclePhaseId
     * @return
     * @throws URISyntaxException
     */
    Integer assignCyclePhaseToCreator(Long cyclePhaseId) throws URISyntaxException;

    /**
     * Get testcases scheduled under this cycle phase.
     * @param cyclePhaseId
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId) throws URISyntaxException;

    /**
     * Execute given releaseTestSchedule ids with given status.
     * @param rtsIds
     * @param executionStatus
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> executeReleaseTestSchedules(Set<Long> rtsIds, String executionStatus) throws URISyntaxException;

    User getCurrentUser();

    /**
     * Clears all data saved in this instance and related to this.
     */
    void clear();
}
