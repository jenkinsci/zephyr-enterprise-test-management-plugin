package com.thed.service.impl;

import com.thed.model.Cycle;
import com.thed.model.CyclePhase;
import com.thed.service.CycleService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 26/6/19.
 */
public class CycleServiceImpl extends BaseServiceImpl implements CycleService {

    public CycleServiceImpl() {
        super();
    }

    @Override
    public List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException {
        return zephyrRestService.getAllCyclesForReleaseId(releaseId);
    }

    @Override
    public Cycle createCycle(Cycle cycle) throws URISyntaxException {
        return zephyrRestService.createCycle(cycle);
    }

    @Override
    public Cycle getCycleById(Long cycleId) throws URISyntaxException {
        return zephyrRestService.getCycleById(cycleId);
    }

    @Override
    public CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException {
        return zephyrRestService.createCyclePhase(cyclePhase);
    }

    @Override
    public Integer assignCyclePhase(Long cyclePhaseId) throws URISyntaxException {
        return zephyrRestService.assignCyclePhase(cyclePhaseId);
    }
}
