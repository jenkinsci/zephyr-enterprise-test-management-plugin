package com.thed.service.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thed.model.*;
import com.thed.service.HttpClientService;
import com.thed.service.ZephyrRestService;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;

/**user
 * Created by prashant on 20/6/19.
 */
public class ZephyrRestServiceImpl implements ZephyrRestService {

    public static final String GET_CURRENT_USER_URL = "/flex/services/rest/{restVersion}/user/current";

    public static final String GET_PROJECT_BY_ID_URL = "/flex/services/rest/{restVersion}/project/{projectId}";
    public static final String GET_ALL_PROJECTS_FOR_CURRENT_USER_URL = "/flex/services/rest/{restVersion}/project/user/{userId}";

    public static final String GET_ALL_RELEASES_FOR_PROJECT_ID_URL = "/flex/services/rest/{restVersion}/release/paged/project/{projectId}"; //?order=id&isascorder=true&isVisible=false

    public static final String GET_TCR_CATALOG_TREE_NODES_URL = "/flex/services/rest/{restVersion}/testcasetree"; //?type=Phase&revisionid=0&releaseid=10
    public static final String GET_TCR_CATALOG_TREE_NODE_URL = "/flex/services/rest/{restVersion}/testcasetree/{tcrCatalogTreeId}";
    public static final String CREATE_TCR_CATALOG_TREE_NODE_URL = "/flex/services/rest/{restVersion}/testcasetree"; //?parentid=0

    public static final String MAP_TESTCASE_TO_REQUIREMENTS_URL = "/flex/services/rest/v3/requirement/bulk";

    public static final String GET_TESTCASES_FOR_TREE_ID_URL = "/flex/services/rest/{restVersion}/testcase/tree/{tcrCatalogTreeId}"; //?offset=0&pagesize=50&dbsearch=true&isascorder=true&order=orderId&frozen=false&is_cfield=false
    public static final String CREATE_TESTCASES_BULK_URL = "/flex/services/rest/{restVersion}/testcase/bulk";

    public static final String GET_CYCLE_BY_ID_URL = "/flex/services/rest/{restVersion}/cycle/{id}";
    public static final String CREATE_CYCLE_URL = "/flex/services/rest/{restVersion}/cycle";
    public static final String GET_ALL_CYCLES_FOR_RELEASE_ID_URL = "/flex/services/rest/{restVersion}/cycle/release/{releaseId}";

    public static final String CREATE_CYCLE_PHASE_URL = "/flex/services/rest/{restVersion}/cycle/{cycleId}/phase";
    public static final String ADD_TESTCASES_TO_FREE_FORM_CYCLE_PHASE_URL = "/flex/services/rest/{restVersion}/assignmenttree/{cyclePhaseId}/assign/bytree/{tcrCatalogTreeId}"; //?includehierarchy=false;
    public static final String ASSIGN_CYCLE_PHASE_URL = "/flex/services/rest/{restVersion}/assignmenttree/{cyclePhaseId}/assign";
    public static final String GET_RELEASE_TEST_SCHEDULES_URL = "/flex/services/rest/{restVersion}/execution"; //?cyclephaseid=11&pagesize=10000;
    public static final String EXECUTE_RELEASE_TEST_SCHEDULES_IN_BULK_URL = "/flex/services/rest/{restVersion}/execution/bulk";//?status=1&testerid=1&allExecutions=false&includeanyoneuser=true

    public static final String UPLOAD_ATTACHMENT_URL = "/flex/upload/document/genericattachment";
    public static final String ADD_ATTACHMENT_URL = "/flex/services/rest/{restVersion}/attachment/list";

    public static final String ADD_TESTSTEP_URL = "/flex/services/rest/{restVersion}/testcase/{testcaseVersionId}/teststep/{tctId}";
    public static final String ADD_TESTSTEP_RESULT_URL = "/flex/services/rest/{restVersion}/execution/teststepresult/saveorupdate";

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

    private String buildUrl(String url, Map<String, String> pathParams, List<NameValuePair> queryParams) throws URISyntaxException {

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
        url = getHostAddress() + url;

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

        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("order", "id"));
        queryParams.add(new BasicNameValuePair("isascorder", "true"));
        queryParams.add(new BasicNameValuePair("isVisible", "false"));

        String url = buildUrl(prepareUrl(GET_ALL_RELEASES_FOR_PROJECT_ID_URL), pathParams, queryParams);

        String res = httpClientService.getRequest(url);
        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("results");
        String resultStr = jsonArray.toString();

        Type releaseListType = new TypeToken<List<Release>>(){}.getType();
        return gson.fromJson(resultStr, releaseListType);
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

    @Override
    public List<TCRCatalogTreeDTO> getTCRCatalogTreeNodes(String type, Long revisionId, Long releaseId) throws URISyntaxException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("type", type));
        queryParams.add(new BasicNameValuePair("revisionid", revisionId.toString()));
        queryParams.add(new BasicNameValuePair("releaseid", releaseId.toString()));

        String url = buildUrl(prepareUrl(GET_TCR_CATALOG_TREE_NODES_URL), null, queryParams);
        String res = httpClientService.getRequest(url);

        Type tcrCatalogTreeListType = new TypeToken<List<TCRCatalogTreeDTO>>(){}.getType();
        return gson.fromJson(res, tcrCatalogTreeListType);
    }

    @Override
    public TCRCatalogTreeDTO getTCRCatalogTreeNode(Long tcrCatalogTreeId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("tcrCatalogTreeId", tcrCatalogTreeId.toString());

        String url = buildUrl(prepareUrl(GET_TCR_CATALOG_TREE_NODE_URL), pathParams, null);
        String res = httpClientService.getRequest(url);

        return gson.fromJson(res, TCRCatalogTreeDTO.class);
    }

    @Override
    public TCRCatalogTreeDTO createTCRCatalogTreeNode(TCRCatalogTreeDTO tcrCatalogTreeDTO) throws URISyntaxException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("parentid", tcrCatalogTreeDTO.getParentId().toString()));
        tcrCatalogTreeDTO.setParentId(null);

        String url = buildUrl(prepareUrl(CREATE_TCR_CATALOG_TREE_NODE_URL), null, queryParams);
        String res = httpClientService.postRequest(url, gson.toJson(tcrCatalogTreeDTO));
        return gson.fromJson(res, TCRCatalogTreeDTO.class);
    }

    @Override
    public List<String> mapTestcaseToRequirements(List<MapTestcaseToRequirement> mapTestcaseToRequirements) throws URISyntaxException {
        String url = buildUrl(prepareUrl(MAP_TESTCASE_TO_REQUIREMENTS_URL), null, null);
        String res = httpClientService.postRequest(url, gson.toJson(mapTestcaseToRequirements));
        Type stringListType = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(res, stringListType);
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

        String url = buildUrl(prepareUrl(GET_TESTCASES_FOR_TREE_ID_URL), pathParams, queryParams);
        String res = httpClientService.getRequest(url);
        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("results");
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
    public String addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, Map<Long, Set<Long>> treeTestcaseMap, Boolean includeHierarchy) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("cyclePhaseId", cyclePhase.getId().toString());
        pathParams.put("tcrCatalogTreeId", cyclePhase.getTcrCatalogTreeId().toString());

        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("includehierarchy", includeHierarchy.toString()));

        JSONArray contentJsonArray = new JSONArray();

        for (Map.Entry<Long, Set<Long>> entry : treeTestcaseMap.entrySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("treeid", entry.getKey());
            jsonObject.put("tctIds", entry.getValue());
            jsonObject.put("isExclusion", Boolean.TRUE);

            contentJsonArray.put(jsonObject);
        }

        String url = buildUrl(prepareUrl(ADD_TESTCASES_TO_FREE_FORM_CYCLE_PHASE_URL), pathParams, queryParams);

        return httpClientService.postRequest(url, contentJsonArray.toString());
    }

    @Override
    public Integer assignCyclePhaseToCreator(Long cyclePhaseId) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("cyclePhaseId", cyclePhaseId.toString());

        String url = buildUrl(prepareUrl(ASSIGN_CYCLE_PHASE_URL), pathParams, null);
        String res = httpClientService.postRequest(url, "");
        return Integer.parseInt(res);
    }

    @Override
    public List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId) throws URISyntaxException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("cyclephaseid", cyclePhaseId.toString()));
        queryParams.add(new BasicNameValuePair("pagesize", "10000"));

        String url = buildUrl(prepareUrl(GET_RELEASE_TEST_SCHEDULES_URL), null, queryParams);
        String res = httpClientService.getRequest(url);

        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("results");
        String resultStr = jsonArray.toString();

        Type releaseTestScheduleListType = new TypeToken<List<ReleaseTestSchedule>>(){}.getType();
        return gson.fromJson(resultStr, releaseTestScheduleListType);
    }

    @Override
    public List<ReleaseTestSchedule> executeReleaseTestSchedules(Set<Long> rtsIds, String executionStatus) throws URISyntaxException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("status", executionStatus));
        queryParams.add(new BasicNameValuePair("testerid", getCurrentUser().getId().toString()));
        queryParams.add(new BasicNameValuePair("allExecutions", "false"));
        queryParams.add(new BasicNameValuePair("includeanyoneuser", "true"));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ids", rtsIds);
        jsonObject.put("serachView", false);

        String url = buildUrl(prepareUrl(EXECUTE_RELEASE_TEST_SCHEDULES_IN_BULK_URL), null, queryParams);
        String res = httpClientService.putRequest(url, jsonObject.toString());

        Type releaseTestScheduleListType = new TypeToken<List<ReleaseTestSchedule>>(){}.getType();
        return gson.fromJson(res, releaseTestScheduleListType);
    }

    @Override
    public List<GenericAttachmentDTO> uploadAttachments(List<GenericAttachmentDTO> attachmentDTOs) throws URISyntaxException {
        String url = buildUrl(prepareUrl(UPLOAD_ATTACHMENT_URL), null, null);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (GenericAttachmentDTO attachmentDTO : attachmentDTOs) {
            builder.addBinaryBody(attachmentDTO.getFieldName(), attachmentDTO.getByteData(), ContentType.getByMimeType(attachmentDTO.getContentType()), attachmentDTO.getFileName());
        }

        String res = httpClientService.postRequest(url, builder.build());
        Type type = new TypeToken<List<GenericAttachmentDTO>>(){}.getType();
        return gson.fromJson(res, type);
    }

    @Override
    public List<Attachment> addAttachment(List<Attachment> attachments) throws URISyntaxException {
        String url = buildUrl(prepareUrl(ADD_ATTACHMENT_URL), null, null);

        String res = httpClientService.postRequest(url, gson.toJson(attachments));
        Type type = new TypeToken<List<Attachment>>(){}.getType();
        return gson.fromJson(res, type);
    }

    @Override
    public TestStep addTestStep(TestStep testStep) throws URISyntaxException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("testcaseVersionId", testStep.getTcId().toString());
        pathParams.put("tctId", testStep.getTctId().toString());
        String url = buildUrl(prepareUrl(ADD_TESTSTEP_URL), pathParams, null);
        String res = httpClientService.postRequest(url, gson.toJson(testStep));
        return gson.fromJson(res, TestStep.class);
    }

    @Override
    public List<TestStepResult> addTestStepsResults(List<TestStepResult> testStepResults) throws URISyntaxException {
        String url = buildUrl(prepareUrl(ADD_TESTSTEP_RESULT_URL), null, null);
        String res = httpClientService.postRequest(url, gson.toJson(testStepResults));
        Type type = new TypeToken<List<TestStepResult>>(){}.getType();
        return gson.fromJson(res, type);
    }

    @Override
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
