package com.thed.service;

import com.thed.model.Project;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 25/6/19.
 */
public interface ProjectService extends BaseService {

    List<Project> getAllProjectsForCurrentUser() throws URISyntaxException;

}
