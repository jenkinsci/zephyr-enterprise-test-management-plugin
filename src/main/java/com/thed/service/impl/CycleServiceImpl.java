package com.thed.service.impl;

import com.thed.model.*;
import com.thed.service.CycleService;
import com.thed.service.ExecutionService;
import com.thed.service.TCRCatalogTreeService;
import com.thed.service.TestcaseService;
import com.thed.utils.ZephyrConstants;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by prashant on 26/6/19.
 */
public class CycleServiceImpl extends BaseServiceImpl implements CycleService {

    private ExecutionService executionService = new ExecutionServiceImpl();
    private TCRCatalogTreeService tcrCatalogTreeService = new TCRCatalogTreeServiceImpl();
    private TestcaseService testcaseService = new TestcaseServiceImpl();

    public CycleServiceImpl() {
        super();
    }

    @Override
    public List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException, IOException {
        return zephyrRestService.getAllCyclesForReleaseId(releaseId);
    }

    @Override
    public Cycle createCycle(Cycle cycle) throws URISyntaxException, IOException {
        return zephyrRestService.createCycle(cycle);
    }

    @Override
    public Cycle getCycleById(Long cycleId) throws URISyntaxException, IOException {
        return zephyrRestService.getCycleById(cycleId);
    }

    @Override
    public CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException, IOException {
        return zephyrRestService.createCyclePhase(cyclePhase);
    }

    @Override
    public Integer assignCyclePhaseToCreator(Long cyclePhaseId) throws URISyntaxException, IOException {
        return zephyrRestService.assignCyclePhaseToCreator(cyclePhaseId);
    }

    @Override
    public List<ReleaseTestSchedule> assignCyclePhaseToUser(CyclePhase cyclePhase, Long userId) throws URISyntaxException, IOException {
        List<ReleaseTestSchedule> rtsList = new ArrayList<>();
        int batchSize = ZephyrConstants.BATCH_SIZE;

        Set<Long> treeIds = tcrCatalogTreeService.getTCRCatalogTreeIdHierarchy(cyclePhase.getTcrCatalogTreeId());

        for(Long treeId : treeIds) {
            for(int pageNo = 0; true; pageNo++) {
                int offset = pageNo * batchSize;
                List<PlanningTestcase> planningTestcaseList = testcaseService.getTestcasesForTreeIdFromPlanning(treeId, offset, batchSize);
                if(planningTestcaseList.isEmpty()) {
                    //no testcases in this tree, skip it
                    break;
                }
                List<Long> tctIdList = planningTestcaseList.stream().map(planningTestcase -> planningTestcase.getTct().getId()).collect(Collectors.toList());
                rtsList.addAll(zephyrRestService.assignTCRCatalogTreeTestcasesToUser(cyclePhase.getId(), treeId, tctIdList, userId));

                if(planningTestcaseList.size() < batchSize) {
                    //no more testcases left in this tree to assign, move to next tree
                    break;
                }
            }
        }
        return rtsList;
    }

    @Override
    public void addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, List<TCRCatalogTreeTestcase> testcases, Boolean includeHierarchy) throws URISyntaxException, IOException {
        //todo: this data parsing loop runs two times, once here and once in ZephyrRestService, need to fix this
        Map<Long, Set<Long>> treeTestcaseMap = new HashMap<>();

        int count = 0;
        for (TCRCatalogTreeTestcase testcase : testcases) {
            if(treeTestcaseMap.containsKey(testcase.getTcrCatalogTreeId())) {
                treeTestcaseMap.get(testcase.getTcrCatalogTreeId()).add(testcase.getTestcase().getId());
            }
            else {
                Set<Long> tctIds = new HashSet<>();
                tctIds.add(testcase.getTestcase().getId());
                treeTestcaseMap.put(testcase.getTcrCatalogTreeId(), tctIds);
            }
            count++;

            if(count == ZephyrConstants.BATCH_SIZE) {
                //batch limit reached, process these testcases
                zephyrRestService.addTestcasesToFreeFormCyclePhase(cyclePhase, treeTestcaseMap, includeHierarchy);

                //testcases processed, clear map and reset count
                treeTestcaseMap = new HashMap<>();
                count = 0;
            }
        }

        if(!treeTestcaseMap.isEmpty()) {
            zephyrRestService.addTestcasesToFreeFormCyclePhase(cyclePhase, treeTestcaseMap, includeHierarchy);
        }
    }
}
