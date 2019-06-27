package com.thed.service.impl;

import com.thed.model.TestCase;
import com.thed.service.TestCaseService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by tarun on 25/6/19.
 */
public class TestCaseServiceImpl extends BaseServiceImpl implements TestCaseService {

    public TestCaseServiceImpl() {
        super();
    }

    @Override
    public List<TestCase> createTestCases(Long projectId, Long releaseId, Long tcrCatalogTreeId, List<String> testNames) throws URISyntaxException {
        return zephyrRestService.createTestCases(projectId, releaseId, tcrCatalogTreeId, testNames);
    }
}
