package com.thed.service;

import com.thed.model.Cycle;
import com.thed.model.CyclePhase;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 26/6/19.
 */
public interface CycleService extends BaseService {

    List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException;

    Cycle createCycle(Cycle cycle) throws URISyntaxException;

    Cycle getCycleById(Long cycleId) throws URISyntaxException;

    CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException;

    Integer assignCyclePhase(Long cyclePhaseId) throws URISyntaxException;

}
