package com.thed.service.impl;

import com.thed.model.Release;
import com.thed.service.ReleaseService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 25/6/19.
 */
public class ReleaseServiceImpl extends BaseServiceImpl implements ReleaseService {

    public ReleaseServiceImpl() {
        super();
    }

    @Override
    public List<Release> getAllReleasesForProjectId(Long projectId) throws URISyntaxException {
        return zephyrRestService.getAllReleasesForProjectId(projectId);
    }

}
