package com.thed.service.impl;

import com.thed.model.TestStep;
import com.thed.model.TestStepResult;
import com.thed.service.TestStepService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 10/1/20.
 */
public class TestStepServiceImpl extends BaseServiceImpl implements TestStepService {

    public TestStepServiceImpl() {
        super();
    }

    @Override
    public TestStep getTestStep(Long testcaseVersionId) throws URISyntaxException {
        return zephyrRestService.getTestStep(testcaseVersionId);
    }

    @Override
    public TestStep addTestStep(TestStep testStep) throws URISyntaxException {
        return zephyrRestService.addTestStep(testStep);
    }

}
