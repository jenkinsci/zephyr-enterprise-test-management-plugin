package com.thed.service.impl;

import com.thed.model.MapTestcaseToRequirement;
import com.thed.service.RequirementService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 4/1/20.
 */
public class RequirementServiceImpl extends BaseServiceImpl implements RequirementService {

    public RequirementServiceImpl() {
        super();
    }

    @Override
    public List<String> mapTestcaseToRequirements(List<MapTestcaseToRequirement> mapTestcaseToRequirements) throws URISyntaxException, IOException {
        return zephyrRestService.mapTestcaseToRequirements(mapTestcaseToRequirements);
    }

}
