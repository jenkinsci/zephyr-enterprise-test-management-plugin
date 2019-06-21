package com.thed.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thed.model.Project;
import com.thed.service.HttpClientService;
import com.thed.service.ZephyrRestService;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by prashant on 20/6/19.
 */
public class ZephyrRestServiceImpl implements ZephyrRestService {

    public static final String GET_CURRENT_USER_URL = "/flex/services/rest/v3/user/current";
    public static final String GET_PROJECT_BY_ID = "/flex/services/rest/v3/project/{projectId}";


    private String hostAddress;

    private HttpClientService httpClientService;

    public ZephyrRestServiceImpl() {
        httpClientService = new HttpClientServiceImpl();
    }

    public String buildUrl(String url, Map<String, String> pathParams, List<NameValuePair> queryParams) throws URISyntaxException {

        if(pathParams != null) {
            for (String key : pathParams.keySet()) {
                url = url.replaceAll("\\{"+key+"\\}", pathParams.get(key));
            }
        }

        if(queryParams != null) {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.setParameters(queryParams);
            url = uriBuilder.build().toString();
        }

        return url;
    }

    @Override
    public Boolean verifyCredentials(String hostAddress, String username, String password) throws URISyntaxException {
        Boolean res = login(hostAddress, username, password);
        if(res == Boolean.TRUE) {
            clear();
        }
        return res;
    }

    @Override
    public Boolean login(String hostAddress, String username, String password) throws URISyntaxException {
        String url = hostAddress + GET_CURRENT_USER_URL;
        String res = httpClientService.authenticationGetRequest(url, username, password);
        if(res != null) {
            setHostAddress(hostAddress);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Project getProjectById(Long projectId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("projectId", String.valueOf(projectId));

        String url = getHostAddress() + GET_PROJECT_BY_ID;
        url = buildUrl(url, pathParams, null);

        String res = httpClientService.getRequest(url);
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(res, Project.class);
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    @Override
    public void clear() {
        setHostAddress("");
        httpClientService.clear();
    }
}
