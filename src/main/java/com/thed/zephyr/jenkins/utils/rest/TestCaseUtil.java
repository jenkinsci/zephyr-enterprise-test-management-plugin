package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.thed.zephyr.jenkins.model.TestCaseModel;
import com.thed.zephyr.jenkins.model.Testcase;

public class TestCaseUtil {

	private static final String URL_SEARCH_TEST_CASES_BY_CRITERIA = "{SERVER}/flex/services/rest/v3/testcase/tree/{tcrCatalogTreeId}?offset=0&pagesize=50&dbsearch=true&isascorder=true&order=orderId&frozen=false&is_cfield=false";
	private static final String URL_CREATE_TEST_CASES_BULK = "{SERVER}/flex/services/rest/latest/testcase/bulk?scheduleId=1";

	public static Map<String, Long> searchTestCaseDetails(Long releaseId, RestClient restClient, String restVersion,
			long tcrCatalogTreeId) {
		Map<String, Long> map = new HashMap<String, Long>();
		try {

			HttpResponse response = null;

			String url = null;
			url = URL_SEARCH_TEST_CASES_BY_CRITERIA.replace("{SERVER}", restClient.getUrl())
					.replace("{REST_VERSION}", restVersion).replace("{tcrCatalogTreeId}", tcrCatalogTreeId + "");
			response = restClient.getHttpclient().execute(new HttpGet(url), restClient.getContext());

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode >= 200 && statusCode < 300) {
				HttpEntity entity = response.getEntity();
				String string = null;

				string = EntityUtils.toString(entity);
				JSONObject searchResults = new JSONObject(string);
				JSONArray tests = searchResults.getJSONArray("results");

				int length = tests.length();
				for (int i = 0; i < length; i++) {
					long rId = tests.getJSONObject(i).getLong("releaseId");
					long testId = tests.getJSONObject(i).getJSONObject("testcase").getLong("id");
					String name = tests.getJSONObject(i).getJSONObject("testcase").getString("name");

					if (releaseId == rId) {
						if (!map.containsKey(name)) {
							map.put(name, testId);
						}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static Map<String, Long> createTestCases(Long projectId, Long releaseId, long tcrCatalogTreeId,
			List<String> testNames, RestClient restClient, String restVersion) {
		Map<String, Long> map = new HashMap<String, Long>();
		try {
			
			List<TestCaseModel> list = new ArrayList<TestCaseModel>();
			for (String string : testNames) {
				
				Testcase t = new Testcase();
				t.setProjectId(projectId);
				t.setReleaseId(releaseId);
				t.setName(string);

				TestCaseModel tm = new TestCaseModel();
				tm.setTcrCatalogTreeId(tcrCatalogTreeId);
				
				tm.setTestcase(t);
				list.add(tm);
			}
			
			Gson gson = new Gson();
			
			String json = gson.toJson(list, list.getClass());
			
			System.out.println(json);

			HttpResponse response = null;
			StringEntity payload = new StringEntity(json,
	                ContentType.APPLICATION_FORM_URLENCODED);

			String url = null;
			url = URL_CREATE_TEST_CASES_BULK.replace("{SERVER}", restClient.getUrl())
					.replace("{REST_VERSION}", restVersion);
			HttpPost req = new HttpPost(url);
			req.setEntity(payload);
			req.setHeader("Accept", "application/json");
			req.setHeader("Content-type", "application/json");
			response = restClient.getHttpclient().execute(req, restClient.getContext());

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode >= 200 && statusCode < 300) {
				HttpEntity entity = response.getEntity();
				String string = null;

				string = EntityUtils.toString(entity);
				JSONArray tests = new JSONArray(string);

				int length = tests.length();
				for (int i = 0; i < length; i++) {
					JSONObject testObject = tests.getJSONObject(i).getJSONObject("testcase");
					long rId =    testObject.getLong("releaseId");
					long testId = testObject.getLong("id");
					String name = testObject.getString("name");

					if (releaseId == rId) {
						if (!map.containsKey(name)) {
							map.put(name, testId);
						}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static void main(String[] args) {
		RestClient rc = new RestClient("http://192.168.11.47", "test.manager", "test.manager");

//		Map<String, Long> searchTestCaseDetails = searchTestCaseDetails(1L, rc, "V3", 35);
//		System.out.println(searchTestCaseDetails);
		
		List<String> testNames = new ArrayList<String>();
		testNames.add("Naresh");
		testNames.add("Arun");
		Map<String, Long> createTestCases = createTestCases(1L, 1L, 34, testNames , rc, "v3");
		System.out.println(createTestCases);
		rc.destroy();
	}
}
