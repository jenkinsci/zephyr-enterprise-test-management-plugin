package com.thed.service;

import com.thed.model.MapTestcaseToRequirement;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 3/1/20.
 */
public interface RequirementService extends BaseService {

    /**
     * Create mappings between testcase and requirements given.
     * @param mapTestcaseToRequirements
     * @return
     * @throws URISyntaxException
     */
    List<String> mapTestcaseToRequirements(List<MapTestcaseToRequirement> mapTestcaseToRequirements) throws URISyntaxException;

}
