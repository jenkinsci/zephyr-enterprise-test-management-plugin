package com.thed.service.impl;

import com.thed.model.Testcase;
import com.thed.service.TestcaseService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by tarun on 25/6/19.
 */
public class TestcaseServiceImpl extends BaseServiceImpl implements TestcaseService {

    public TestcaseServiceImpl() {
        super();
    }

    @Override
    public List<Testcase> createTestCases(Long projectId, Long releaseId, Long tcrCatalogTreeId, List<String> testNames) throws URISyntaxException {
        return zephyrRestService.createTestcases(projectId, releaseId, tcrCatalogTreeId, testNames);
    }
}
