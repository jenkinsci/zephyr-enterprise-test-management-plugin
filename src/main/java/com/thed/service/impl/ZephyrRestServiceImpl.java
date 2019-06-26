package com.thed.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thed.model.Cycle;
import com.thed.model.Project;
import com.thed.model.TestCase;
import com.thed.model.User;
import com.thed.service.HttpClientService;
import com.thed.service.ZephyrRestService;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**user
 * Created by prashant on 20/6/19.
 */
public class ZephyrRestServiceImpl implements ZephyrRestService {

    public static final String GET_CURRENT_USER_URL = "/flex/services/rest/{restVersion}/user/current";

    public static final String GET_PROJECT_BY_ID_URL = "/flex/services/rest/{restVersion}/project/{projectId}";

    public static final String GET_ALL_PROJECTS_FOR_CURRENT_USER = "/flex/services/rest/{restVersion}/project/user/{userId}";

//    public static final String GET_CYCLE_BY_ID_URL = "/flex/services/rest/{restVersion}/cycle/{id}";
    public static final String CREATE_CYCLE_URL = "/flex/services/rest/{restVersion}/cycle";

    public static final String URL_CREATE_TEST_CASES_BULK = "/flex/services/rest/{restVersion}/testcase/bulk?scheduleId=1";


    private User currentUser;
    private String hostAddress;
    private String restVersion = "v3";

    private HttpClientService httpClientService = new HttpClientServiceImpl();
    private Gson gson;

    public ZephyrRestServiceImpl() {
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
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

    private String prepareUrl(String url) throws URISyntaxException {
        url = hostAddress + url;

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("restVersion", restVersion);
        return buildUrl(url, pathParams, null);
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
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("restVersion", restVersion);
        url = buildUrl(url, pathParams, null);
        String res = httpClientService.authenticationGetRequest(url, username, password);
        if(res != null) {
            setCurrentUser(gson.fromJson(res, User.class));
            setHostAddress(hostAddress);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Project getProjectById(Long projectId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("projectId", String.valueOf(projectId));

        String url = prepareUrl(GET_PROJECT_BY_ID_URL);
        url = buildUrl(url, pathParams, null);
        String res = httpClientService.getRequest(url);
        return gson.fromJson(res, Project.class);
    }

    @Override
    public Cycle createCycle(Cycle cycle) throws URISyntaxException {
        String url = prepareUrl(CREATE_CYCLE_URL);
        String res = httpClientService.postRequest(url, gson.toJson(cycle));
        return gson.fromJson(res, Cycle.class);
    }

    public List<Project> getAllProjectsForCurrentUser() throws URISyntaxException {
        if(currentUser == null) {
            return null;
        }

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("userId", currentUser.getId().toString());
        String url = buildUrl(prepareUrl(GET_ALL_PROJECTS_FOR_CURRENT_USER), pathParams, null);

        String res = httpClientService.getRequest(url);

        Type projectListType = new TypeToken<List<Project>>(){}.getType();

        return gson.fromJson(res, projectListType);
    }

    @Override
    public List<TestCase> createTestCases(Long projectId, Long releaseId, Long tcrCatalogTreeId, List<String> testNames) throws URISyntaxException {
        List<TestCase> result = new ArrayList<>();

        //Create the payload for request
        List<TestCase> list = new ArrayList<>();
        for (String string : testNames) {
            TestCase t = new TestCase();
            t.setProjectId(projectId);
            t.setReleaseId(releaseId);
            t.setName(string);
            t.setTcrCatalogTreeId(tcrCatalogTreeId);
            list.add(t);
        }
        Gson gson = new Gson();
        String json = gson.toJson(list, list.getClass());

        String url = prepareUrl(URL_CREATE_TEST_CASES_BULK);
        String res = httpClientService.postRequest(url, json);
        JSONArray tests = new JSONArray(res);

        int length = tests.length();
        for (int i = 0; i < length; i++) {
            JSONObject testObject = tests.getJSONObject(i).getJSONObject("testcase");
            Long relId = testObject.getLong("releaseId");
            Long testId = testObject.getLong("id");
            final String name = testObject.getString("name");

            if (releaseId == relId) {
                TestCase tc = result.stream().filter(x -> testId.equals(x.getId())).findAny().orElse(null);
                if(tc == null){
                    TestCase testCase = new TestCase();
                    testCase.setId(testId);
                    testCase.setName(name);
                    result.add(testCase);
                }
            }
        }
        return result;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getRestVersion() {
        return restVersion;
    }

    public void setRestVersion(String restVersion) {
        this.restVersion = restVersion;
    }

    @Override
    public void clear() {
        setCurrentUser(null);
        setHostAddress("");
        httpClientService.clear();
    }
}
