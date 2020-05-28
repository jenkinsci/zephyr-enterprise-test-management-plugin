package com.thed.service;

import com.thed.model.Project;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 25/6/19.
 */
public interface ProjectService extends BaseService {

    List<Project> getAllProjectsForCurrentUser() throws URISyntaxException, IOException;

    Project getProjectById(Long projectId) throws URISyntaxException, IOException;

    /**
     * Return no of days from startDate to endDate of the project. Returns -1 if no endDate specified.
     * @param projectId
     * @return
     * @throws URISyntaxException
     */
    Long getProjectDurationInDays(Long projectId) throws URISyntaxException, IOException;

}
