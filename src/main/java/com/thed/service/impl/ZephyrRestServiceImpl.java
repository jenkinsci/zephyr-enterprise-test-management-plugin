package com.thed.service.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thed.model.*;
import com.thed.service.HttpClientService;
import com.thed.service.ZephyrRestService;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
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

    public static final String GET_TCR_CATALOG_TREE_NODES_URL = "/flex/services/rest/{restVersion}/testcasetree"; //?type=Phase&revisionid=0&releaseid=10
    public static final String CREATE_TCR_CATALOG_TREE_NODE_URL = "/flex/services/rest/{restVersion}/testcasetree"; //?parentid=0

    public static final String GET_TESTCASES_FOR_TREE_ID = "flex/services/rest/{restVersion}/testcase/tree/{tcrCatalogTreeId}"; //?offset=0&pagesize=50&dbsearch=true&isascorder=true&order=orderId&frozen=false&is_cfield=false
    public static final String CREATE_TESTCASES_BULK_URL = "/flex/services/rest/{restVersion}/testcase/bulk";

    public static final String GET_CYCLE_BY_ID_URL = "/flex/services/rest/{restVersion}/cycle/{id}";
    public static final String CREATE_CYCLE_URL = "/flex/services/rest/{restVersion}/cycle";
    public static final String GET_ALL_CYCLES_FOR_RELEASE_ID_URL = "/flex/services/rest/{restVersion}/cycle/release/{releaseId}";

    public static final String CREATE_CYCLE_PHASE_URL = "/flex/services/rest/{restVersion}/cycle/{cycleId}/phase";
    public static final String ASSIGN_CYCLE_PHASE_URL = "/flex/services/rest/{restVersion}/assignmenttree/{cyclephaseid}/assign";
    public static final String GET_RELEASE_TEST_SCHEDULES_URL = "/flex/services/rest/v3/execution"; //?cyclephaseid=11&pagesize=10000;

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

    @Override
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

    @Override
    public List<Release> getAllReleasesForProjectId(Long projectId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("projectId", projectId.toString());
        String url = buildUrl(prepareUrl(GET_ALL_RELEASES_FOR_PROJECT_ID_URL), pathParams, null);

        String res = httpClientService.getRequest(url);

        Type releaseListType = new TypeToken<List<Release>>(){}.getType();
        return gson.fromJson(res, releaseListType);
    }

    @Override
    public List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("releaseId", releaseId.toString());

        String url = buildUrl(prepareUrl(GET_ALL_CYCLES_FOR_RELEASE_ID_URL), pathParams, null);
        String res = httpClientService.getRequest(url);

        Type releaseListType = new TypeToken<List<Cycle>>(){}.getType();
        return gson.fromJson(res, releaseListType);
    }

    public List<TCRCatalogTreeDTO> getTCRCatalogTreeNodes(String type, Long revisionId, Long releaseId) throws URISyntaxException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("type", type));
        queryParams.add(new BasicNameValuePair("revisionId", revisionId.toString()));
        queryParams.add(new BasicNameValuePair("releaseId", releaseId.toString()));

        String url = buildUrl(prepareUrl(GET_TCR_CATALOG_TREE_NODES_URL), null, queryParams);
        String res = httpClientService.getRequest(url);

        Type tcrCatalogTreeListType = new TypeToken<List<TCRCatalogTreeDTO>>(){}.getType();
        return gson.fromJson(res, tcrCatalogTreeListType);
    }

    @Override
    public TCRCatalogTreeDTO createTCRCatalogTreeNode(TCRCatalogTreeDTO tcrCatalogTreeDTO) throws URISyntaxException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("parentId", tcrCatalogTreeDTO.getParentId().toString()));
        tcrCatalogTreeDTO.setParentId(null);

        String url = buildUrl(prepareUrl(CREATE_TCR_CATALOG_TREE_NODE_URL), null, queryParams);
        String res = httpClientService.postRequest(url, gson.toJson(tcrCatalogTreeDTO));
        return gson.fromJson(res, TCRCatalogTreeDTO.class);
    }

    @Override
    public List<TCRCatalogTreeTestcase> getTestcasesForTreeId(Long tcrCatalogTreeId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("tcrCatalogTreeId", tcrCatalogTreeId.toString());

        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("offset", "0"));
        queryParams.add(new BasicNameValuePair("pagesize", "1000"));
        queryParams.add(new BasicNameValuePair("dbsearch", "true"));
        queryParams.add(new BasicNameValuePair("isascorder", "true"));
        queryParams.add(new BasicNameValuePair("order", "orderId"));
        queryParams.add(new BasicNameValuePair("frozen", "false"));
        queryParams.add(new BasicNameValuePair("is_cfield", "false"));

        String url = buildUrl(prepareUrl(GET_TESTCASES_FOR_TREE_ID), pathParams, queryParams);
        String res = httpClientService.getRequest(url);
        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("result");
        String resultStr = jsonArray.toString();

        Type tcrCatalogTreeTestcaseListType = new TypeToken<List<TCRCatalogTreeTestcase>>(){}.getType();
        return gson.fromJson(resultStr, tcrCatalogTreeTestcaseListType);
    }

    @Override
    public List<TCRCatalogTreeTestcase> createTestcases(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcases) throws URISyntaxException {
        String url = prepareUrl(CREATE_TESTCASES_BULK_URL);
        String res = httpClientService.postRequest(url, gson.toJson(tcrCatalogTreeTestcases));

        Type tcrCatalogTreeTestcaseListType = new TypeToken<List<TCRCatalogTreeTestcase>>(){}.getType();
        return gson.fromJson(res, tcrCatalogTreeTestcaseListType);
    }

    @Override
    public Cycle getCycleById(Long cycleId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", cycleId.toString());

        String url = buildUrl(prepareUrl(GET_CYCLE_BY_ID_URL), pathParams, null);
        String res = httpClientService.getRequest(url);
        return gson.fromJson(res, Cycle.class);
    }

    @Override
    public CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("cycleId", cyclePhase.getCycleId().toString());

        String url = buildUrl(prepareUrl(CREATE_CYCLE_PHASE_URL), pathParams, null);
        String res = httpClientService.postRequest(url, gson.toJson(cyclePhase));
        return gson.fromJson(res, CyclePhase.class);
    }

    @Override
    public Integer assignCyclePhase(Long cyclePhaseId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("cyclePhaseId", cyclePhaseId.toString());

        String url = buildUrl(prepareUrl(ASSIGN_CYCLE_PHASE_URL), pathParams, null);
        String res = httpClientService.postRequest(url, "");
        return Integer.parseInt(res);
    }

    public void getReleaseTestSchedules(Long cyclePhaseId) throws URISyntaxException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("cyclephaseid", cyclePhaseId.toString()));
        queryParams.add(new BasicNameValuePair("pagesize", "10000"));

        String url = buildUrl(prepareUrl(GET_RELEASE_TEST_SCHEDULES_URL), null, queryParams);
        String res = httpClientService.getRequest(url);
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
