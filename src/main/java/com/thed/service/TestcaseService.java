package com.thed.service;

import com.thed.model.Testcase;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by tarun on 25/6/19.
 */
public interface TestcaseService extends BaseService {

    /**
     * Create Test Cases
     * @param projectId
     * @param releaseId
     * @param tcrCatalogTreeId
     * @param testNames
     * @return
     * @throws java.net.URISyntaxException
     */
    List<Testcase> createTestCases(Long projectId, Long releaseId, Long tcrCatalogTreeId, List<String> testNames) throws URISyntaxException;

}
