package com.thed.service;

import com.thed.model.Cycle;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 26/6/19.
 */
public interface CycleService extends BaseService {

    List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException;

}
