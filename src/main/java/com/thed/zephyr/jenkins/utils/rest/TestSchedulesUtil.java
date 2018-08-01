package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class TestSchedulesUtil {

	private static final String URL_SEARCH_TEST_CASES_BY_CRITERIA = "{SERVER}/flex/services/rest/v3/execution?cyclephaseid={CYCLE_PHASE_ID}&pagesize=10000";

	public static Map<Long, Long> searchTestScheduleDetails(Long cyclePhaseId, RestClient restClient,
			String restVersion) {
		Map<Long, Long> remoteTestcaseIdTestScheduleIdMap = new HashMap<Long, Long>();
		try {
			HttpResponse response = null;

			String url = null;
			url = URL_SEARCH_TEST_CASES_BY_CRITERIA.replace("{SERVER}", restClient.getUrl())
					.replace("{REST_VERSION}", restVersion).replace("{CYCLE_PHASE_ID}", cyclePhaseId + "");
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
					long executionId = tests.getJSONObject(i).getLong("id");
					long testcaseId = tests.getJSONObject(i).getJSONObject("tcrTreeTestcase").getJSONObject("testcase")
							.getLong("id");

					System.out.println(executionId + ":" + testcaseId);

					remoteTestcaseIdTestScheduleIdMap.put(testcaseId, executionId);

				}

			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			restClient.destroy();
		}

		return remoteTestcaseIdTestScheduleIdMap;

	}

	public static void main(String[] args) {
		RestClient rc = new RestClient("http://192.168.11.47", "test.manager", "test.manager");

		searchTestScheduleDetails(5L, rc, "V3");
		rc.destroy();
	}
}
