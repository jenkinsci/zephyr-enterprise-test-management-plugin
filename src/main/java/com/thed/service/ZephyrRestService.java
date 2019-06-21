package com.thed.service;

import com.thed.model.Project;

import java.net.URISyntaxException;

/**
 * Created by prashant on 20/6/19.
 */
public interface ZephyrRestService {

    /**
     * Verifies given credentials
     * @param hostUrl
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public Boolean verifyCredentials(String hostUrl, String username, String password) throws URISyntaxException;

    /**
     * Verifies given credentials and stores hostAddress if verification succeeds
     * @param hostAddress
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    Boolean login(String hostAddress, String username, String password) throws URISyntaxException;

    /**
     * Get project using project id.
     * @param projectId
     * @return
     * @throws URISyntaxException
     */
    Project getProjectById(Long projectId) throws URISyntaxException;

    /**
     * Clears all data saved in this instance and related to this.
     */
    void clear();
}
