package com.thed.service.impl;

import com.thed.model.Cycle;
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
}
