package com.thed.service;

import com.thed.model.Cycle;
import com.thed.model.CyclePhase;
import com.thed.model.ReleaseTestSchedule;
import com.thed.model.TCRCatalogTreeTestcase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * Created by prashant on 26/6/19.
 */
public interface CycleService extends BaseService {

    List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException, IOException;

    Cycle createCycle(Cycle cycle) throws URISyntaxException, IOException;

    Cycle getCycleById(Long cycleId) throws URISyntaxException, IOException;

    CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException, IOException;

    Integer assignCyclePhaseToCreator(Long cyclePhaseId) throws URISyntaxException, IOException;

    /**
     * Assign testcases in given cyclePhase to given user.
     * @param cyclePhase
     * @param userId
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> assignCyclePhaseToUser(CyclePhase cyclePhase, Long userId, Set<Long> additionalTreeIds) throws URISyntaxException, IOException;

    Set<Long> addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, List<TCRCatalogTreeTestcase> testcases, Boolean includeHierarchy) throws URISyntaxException, IOException;

}
