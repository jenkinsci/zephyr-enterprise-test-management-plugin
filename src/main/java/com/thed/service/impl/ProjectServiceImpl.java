package com.thed.service.impl;

import com.thed.model.Project;
import com.thed.service.ProjectService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 25/6/19.
 */
public class ProjectServiceImpl extends BaseServiceImpl implements ProjectService {

    public ProjectServiceImpl() {
        super();
    }

    @Override
    public List<Project> getAllProjectsForCurrentUser() throws URISyntaxException {
        return zephyrRestService.getAllProjectsForCurrentUser();
    }
}
