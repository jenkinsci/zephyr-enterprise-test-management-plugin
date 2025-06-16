package com.thed.service;

import com.thed.model.PreferenceDTO;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

public interface PreferenceService extends BaseService {

    PreferenceDTO getPreference(String key) throws IOException, URISyntaxException;

    PreferenceDTO getTestcaseExecutionStatusPreference() throws IOException, URISyntaxException;

    Set<String> getTestcaseExecutionStatusIds(Boolean searchActive) throws IOException, URISyntaxException;

    boolean isEnvironmentEnabled() throws IOException, URISyntaxException;

}
