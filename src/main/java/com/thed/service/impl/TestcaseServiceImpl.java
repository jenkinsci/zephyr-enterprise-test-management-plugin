package com.thed.service.impl;

import com.thed.model.TCRCatalogTreeTestcase;
import com.thed.model.Testcase;
import com.thed.service.TestcaseService;
import hudson.tasks.junit.CaseResult;
import org.jaxen.pantry.Test;

import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by tarun on 25/6/19.
 */
public class TestcaseServiceImpl extends BaseServiceImpl implements TestcaseService {

    public TestcaseServiceImpl() {
        super();
    }

    private Testcase parse(CaseResult caseResult) {
        Testcase testcase = new Testcase();
        testcase.setName(caseResult.getFullName());
        testcase.setAutomated(true);
        testcase.setScriptName("Created By Jenkins");
        return testcase;
    }

    @Override
    public List<TCRCatalogTreeTestcase> getTestcasesForTreeId(Long tcrCatalogTreeId) throws URISyntaxException {
        return zephyrRestService.getTestcasesForTreeId(tcrCatalogTreeId);
    }

    @Override
    public List<TCRCatalogTreeTestcase> createTestcases(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcases) throws URISyntaxException {
        return zephyrRestService.createTestcases(tcrCatalogTreeTestcases);
    }

    @Override
    public Map<CaseResult, TCRCatalogTreeTestcase> createTestcases(Map<Long, List<CaseResult>> treeIdCaseResultMap) throws URISyntaxException {

        if(treeIdCaseResultMap == null || treeIdCaseResultMap.isEmpty()) {
            return new HashMap<>();
        }

        List<CaseResult> caseResultList = new ArrayList<>();
        List<TCRCatalogTreeTestcase> treeTestcases = new ArrayList<>();
        Set<Long> treeIds = treeIdCaseResultMap.keySet();

        for (Long treeId : treeIds) {

            List<CaseResult> caseResults = treeIdCaseResultMap.get(treeId);

            caseResultList.addAll(caseResults);

            for(CaseResult caseResult : caseResults) {
                TCRCatalogTreeTestcase tcrCatalogTreeTestcase = new TCRCatalogTreeTestcase();
                tcrCatalogTreeTestcase.setTcrCatalogTreeId(treeId);
                tcrCatalogTreeTestcase.setTestcase(parse(caseResult));

                treeTestcases.add(tcrCatalogTreeTestcase);
            }
        }

        treeTestcases = createTestcases(treeTestcases);

        Map<CaseResult, TCRCatalogTreeTestcase> map = new HashMap<>();

        loop1 : for (CaseResult caseResult : caseResultList) {
            for (TCRCatalogTreeTestcase tcrCatalogTreeTestcase : treeTestcases) {
                if(caseResult.getFullName().equals(tcrCatalogTreeTestcase.getTestcase().getName())) {
                    map.put(caseResult, tcrCatalogTreeTestcase);
                    continue loop1;
                }
            }
        }

        return map;
    }

    @Override
    public void createTestcasesWithList(Map<Long, List<Testcase>> treeIdTestcaseMap) throws URISyntaxException {
        List<TCRCatalogTreeTestcase> treeTestcases = new ArrayList<>();
        Set<Long> treeIds = treeIdTestcaseMap.keySet();

        for (Long treeId : treeIds) {
            List<Testcase> testcaseList = treeIdTestcaseMap.get(treeId);
            for (Testcase testcase : testcaseList) {
                TCRCatalogTreeTestcase tcrCatalogTreeTestcase = new TCRCatalogTreeTestcase();
                tcrCatalogTreeTestcase.setTcrCatalogTreeId(treeId);
                tcrCatalogTreeTestcase.setTestcase(testcase);

                treeTestcases.add(tcrCatalogTreeTestcase);
            }
        }

        treeTestcases = createTestcases(treeTestcases);
    }
}
