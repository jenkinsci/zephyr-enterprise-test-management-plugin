package com.thed.service.impl;

import com.thed.model.ExecutionRequest;
import com.thed.model.ReleaseTestSchedule;
import com.thed.model.TestStepResult;
import com.thed.service.ExecutionService;
import com.thed.service.UserService;
import com.thed.utils.ZephyrConstants;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by prashant on 29/6/19.
 */
public class ExecutionServiceImpl extends BaseServiceImpl implements ExecutionService {

    private UserService userService = new UserServiceImpl();

    public ExecutionServiceImpl() {
        super();
    }

    @Override
    public List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId) throws URISyntaxException, IOException {
        return zephyrRestService.getReleaseTestSchedules(cyclePhaseId, null, 0);
    }

    @Override
    public List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId, Integer offset, Integer pageSize) throws URISyntaxException, IOException {
        return zephyrRestService.getReleaseTestSchedules(cyclePhaseId, offset, pageSize);
    }

    @Override
    public List<ReleaseTestSchedule> executeReleaseTestSchedules(Set<Long> rtsIds, String statusId) throws URISyntaxException, IOException {
        List<ReleaseTestSchedule> rtsList = new ArrayList<>();
        if(rtsIds.isEmpty()) {
            return rtsList;
        }

        Set<Long> tempIds = new HashSet<>();
        for(Long rtsId : rtsIds) {
            tempIds.add(rtsId);
            if(tempIds.size() == ZephyrConstants.BATCH_SIZE) {
                rtsList.addAll(zephyrRestService.executeReleaseTestSchedules(tempIds, statusId));
                tempIds.clear();
            }
        }

        if(!tempIds.isEmpty()) {
            rtsList.addAll(zephyrRestService.executeReleaseTestSchedules(tempIds, statusId));
        }

        return rtsList;
    }

    @Override
    public List<TestStepResult> addTestStepResults(List<TestStepResult> testStepResults) throws URISyntaxException, IOException {
        return zephyrRestService.addTestStepsResults(testStepResults);
    }

    @Override
    public List<ReleaseTestSchedule> execute(List<ExecutionRequest> executionRequestList) throws IOException, URISyntaxException {
        executionRequestList.forEach(er -> {
            if(er.getTesterId() == null) {
                er.setTesterId(userService.getCurrentUser().getId());
            }
        });
        return zephyrRestService.execute(executionRequestList);
    }

}
