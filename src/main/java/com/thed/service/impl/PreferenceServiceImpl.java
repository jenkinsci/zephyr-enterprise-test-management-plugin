package com.thed.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thed.model.PreferenceDTO;
import com.thed.service.PreferenceService;
import com.thed.utils.GsonUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PreferenceServiceImpl extends BaseServiceImpl implements PreferenceService {

    private final static String TESTCASE_EXECUTION_STATUS_PREFERENCE_KEY = "testresult.testresultStatus.LOV";

    @Override
    public PreferenceDTO getPreference(String key) throws IOException, URISyntaxException {
        return zephyrRestService.getPreference(key);
    }

    @Override
    public PreferenceDTO getTestcaseExecutionStatusPreference() throws IOException, URISyntaxException {
        return getPreference(TESTCASE_EXECUTION_STATUS_PREFERENCE_KEY);
    }

    @Override
    public Set<String> getTestcaseExecutionStatusIds(Boolean searchActive) throws IOException, URISyntaxException {
        PreferenceDTO preferenceDTO = getTestcaseExecutionStatusPreference();

        Set<String> statusIds = new HashSet<>();

        if(preferenceDTO != null) {
            Type mapListType = new TypeToken<List<Map>>(){}.getType();
            List<Map> mapList = GsonUtil.CUSTOM_GSON.fromJson(preferenceDTO.getValue(), mapListType);

            for(Map map : mapList) {
                if(map.containsKey("id")) {
                    String id = map.get("id").toString();
                    if(searchActive != null && map.containsKey("active")) {
                        Boolean active = Boolean.valueOf(map.get("active").toString());
                        if(searchActive == active) {
                            statusIds.add(id);
                        }
                    } else {
                        statusIds.add(id);
                    }
                }
            }

        }
        return statusIds;
    }

}