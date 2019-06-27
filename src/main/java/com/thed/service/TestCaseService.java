package com.thed.service;

import com.thed.model.TestCase;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by tarun on 25/6/19.
 */
public interface TestCaseService extends BaseService {

    /**
     * Create Test Cases
     * @param projectId
     * @param releaseId
     * @param tcrCatalogTreeId
     * @param testNames
     * @return
     * @throws java.net.URISyntaxException
     */
    List<TestCase> createTestCases(Long projectId, Long releaseId, Long tcrCatalogTreeId, List<String> testNames) throws URISyntaxException;

}
