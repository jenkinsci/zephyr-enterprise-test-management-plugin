package com.thed.service;

import com.thed.model.TestStep;
import com.thed.model.TestStepResult;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 10/1/20.
 */
public interface TestStepService extends BaseService {

    /**
     * Add testStep to single testcase.
     * @param testStep
     * @return
     * @throws URISyntaxException
     */
    TestStep addTestStep(TestStep testStep) throws URISyntaxException;

}
