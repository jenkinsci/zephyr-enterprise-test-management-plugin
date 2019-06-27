package com.thed.service.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thed.model.Cycle;
import com.thed.model.Project;
import com.thed.model.Release;
import com.thed.model.Testcase;
import com.thed.model.User;
import com.thed.service.HttpClientService;
import com.thed.service.ZephyrRestService;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Date;
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
    public static final String GET_ALL_PROJECTS_FOR_CURRENT_USER_URL = "/flex/services/rest/{restVersion}/project/user/{userId}";

    public static final String GET_ALL_RELEASES_FOR_PROJECT_ID_URL = "/flex/services/rest/{restVersion}/release/project/{projectId}";

//    public static final String GET_CYCLE_BY_ID_URL = "/flex/services/rest/{restVersion}/cycle/{id}";
    public static final String CREATE_CYCLE_URL = "/flex/services/rest/{restVersion}/cycle";
    public static final String GET_ALL_CYCLES_FOR_RELEASE_ID_URL = "/flex/services/rest/{restVersion}/cycle/release/{releaseId}";

    public static final String URL_CREATE_TEST_CASES_BULK = "/flex/services/rest/{restVersion}/testcase/bulk?scheduleId=1";

    private User currentUser;
    private String hostAddress;
    private String restVersion = "v3";

    private HttpClientService httpClientService = new HttpClientServiceImpl();
    private Gson gson;

    public ZephyrRestServiceImpl() {

        JsonSerializer<Date> ser = new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                    context) {
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        };

        JsonDeserializer<Date> deser = new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context) throws JsonParseException {
                return json == null ? null : new Date(json.getAsLong());
            }
        };

        GsonBuilder builder = new GsonBuilder();
        gson = builder.registerTypeAdapter(Date.class, ser)
                .registerTypeAdapter(Date.class, deser).create();
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
        String url = buildUrl(prepareUrl(GET_ALL_PROJECTS_FOR_CURRENT_USER_URL), pathParams, null);

        String res = httpClientService.getRequest(url);

        Type projectListType = new TypeToken<List<Project>>(){}.getType();
        return gson.fromJson(res, projectListType);
    }

    public List<Release> getAllReleasesForProjectId(Long projectId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("projectId", projectId.toString());
        String url = buildUrl(prepareUrl(GET_ALL_RELEASES_FOR_PROJECT_ID_URL), pathParams, null);

        String res = httpClientService.getRequest(url);

        Type releaseListType = new TypeToken<List<Release>>(){}.getType();
        return gson.fromJson(res, releaseListType);
    }

    public List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("releaseId", releaseId.toString());

        String url = buildUrl(prepareUrl(GET_ALL_CYCLES_FOR_RELEASE_ID_URL), pathParams, null);
        String res = httpClientService.getRequest(url);

        Type releaseListType = new TypeToken<List<Cycle>>(){}.getType();
        return gson.fromJson(res, releaseListType);

    }

    @Override
    public List<Testcase> createTestCases(Long projectId, Long releaseId, Long tcrCatalogTreeId, List<String> testNames) throws URISyntaxException {
        List<Testcase> result = new ArrayList<>();

        //Create the payload for request
        List<Testcase> list = new ArrayList<>();
        for (String string : testNames) {
            Testcase t = new Testcase();
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

            Boolean testExist = Boolean.FALSE;
            if (releaseId == relId) {
                for(Testcase tc : result){
                    if(testId.equals(tc.getId())){
                        testExist = Boolean.TRUE;
                        break;
                    }
                }
                //TestCase tc = result.stream().filter(x -> testId.equals(x.getId())).findAny().orElse(null);
                if(!testExist){
                    Testcase testcase = new Testcase();
                    testcase.setId(testId);
                    testcase.setName(name);
                    result.add(testcase);
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
