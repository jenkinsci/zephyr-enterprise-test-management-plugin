package com.thed.service;

import com.thed.model.Release;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 25/6/19.
 */
public interface ReleaseService extends BaseService {

    /**
     * Get all releases for given projectId.
     * @param projectId
     * @return
     * @throws URISyntaxException
     */
    List<Release> getAllReleasesForProjectId(Long projectId) throws URISyntaxException, IOException;

}
