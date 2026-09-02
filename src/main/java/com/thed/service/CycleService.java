package com.thed.service;

import com.thed.model.Cycle;
import com.thed.model.CyclePhase;
import com.thed.model.ReleaseTestSchedule;
import com.thed.model.TCRCatalogTreeTestcase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
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
     * @param additionalTreeIds tree ids (e.g. package sub-folders just created under the cycle
     *                          phase) that are already known to hold testcases and should be
     *                          queried directly, in addition to whatever the server's tree
     *                          hierarchy lookup returns.
     */
    List<ReleaseTestSchedule> assignCyclePhaseToUser(CyclePhase cyclePhase, Long userId, Set<Long> additionalTreeIds) throws URISyntaxException, IOException;

    /**
     * Adds testcases to the free-form cycle phase.
     * @return the set of TCRCatalogTree ids (including any newly created package sub-folders)
     *         that the server reports as part of the resulting frozen tree, so callers don't
     *         have to rely on a possibly-lagging/unindexed hierarchy lookup to find them.
     */
    Set<Long> addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, List<TCRCatalogTreeTestcase> testcases, Boolean includeHierarchy) throws URISyntaxException, IOException;

}
