package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Cycle {

	private static String URL_GET_CYCLES = "{SERVER}/flex/services/rest/{REST_VERSION}/cycle";
	private static String URL_GET_CYCLE = "{SERVER}/flex/services/rest/{REST_VERSION}/cycle/{CYCLE_ID}";
	private static String URL_UPDATE_CYCLE = "{SERVER}/flex/services/rest/{REST_VERSION}/cycle/{CYCLE_ID}";

	
	public static Long getCycleIdByCycleNameAndReleaseId(String cycleName, Long releaseId, RestClient restClient, String restVersion) {

		Long cycleId = 0L;

		String url = null;
		try {
			url = URL_GET_CYCLES.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion) + "?name=" + URLEncoder.encode(cycleName, "utf-8") + "&releaseId=" + releaseId;
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		HttpResponse response = null;
		try {
			response = restClient.getHttpclient().execute(new HttpGet(url), restClient.getContext());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			try {
				JSONArray cycleArray = new JSONArray(string);
				List<Long> cycleIdList = new ArrayList<Long>();
				for(int i = 0; i < cycleArray.length(); i++) {
					Long id = cycleArray.getJSONObject(i).getLong("id");
					cycleIdList.add(id);
				}
				
				Collections.sort(cycleIdList);
				cycleId = cycleIdList.get(0);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
		}
	
		return cycleId;
	}
	
	public static Map<Long, String> getAllCyclesByReleaseID(Long releaseID, RestClient restClient, String restVersion) {


		Map<Long, String> cycles = new TreeMap<Long, String>();
		
		HttpResponse response = null;
		
		final String url = URL_GET_CYCLES.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion) + "?releaseId=" + releaseID;
		try {
			response = restClient.getHttpclient().execute(new HttpGet(url), restClient.getContext());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				JSONArray cyclesArray = new JSONArray(string);
				for(int i = 0; i < cyclesArray.length(); i++) {
					JSONObject cycleObject = cyclesArray.getJSONObject(i);
					
					int visibility = cycleObject.getInt("status");
					if (visibility == 1) {
						continue;
					}

					Long id = cycleObject.getLong("id");
					String projName = cycleObject.getString("name");
					cycles.put(id, projName);
				}
				
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
		} else {
			
			cycles.put(0L, "No Cycle");
		}
	
		return cycles;
	}
	
	
	public static String getCycleById(Long cycleID, RestClient restClient, String restVersion) {


		String cycleInfo = null;
		HttpResponse response = null;
		
		final String url = URL_GET_CYCLE.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion).replace("{CYCLE_ID}", cycleID+"");
		try {
			response = restClient.getHttpclient().execute(new HttpGet(url), restClient.getContext());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			try {
				cycleInfo = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	
		return cycleInfo;
	}

	
	
	public static Map<String, Long> updateCycle(Long cycleID, int buildId, RestClient restClient, String restVersion) {
		Map<String, Long> map = new HashMap<String, Long>();
		try {
			
			String cycleInfo = getCycleById(cycleID, restClient, restVersion);
			JSONObject cycle = new JSONObject(cycleInfo);
			
			JSONObject jObj = new JSONObject();
			jObj.put("name", cycle.getString("name"));
			jObj.put("releaseId", cycle.getInt("releaseId"));
			jObj.put("build", buildId);
			
			String json = jObj.toString();
			
			HttpResponse response = null;
			StringEntity payload = new StringEntity(json,
	                ContentType.APPLICATION_FORM_URLENCODED);

			String url = null;
			url = URL_UPDATE_CYCLE.replace("{SERVER}", restClient.getUrl())
					.replace("{REST_VERSION}", restVersion).replace("{CYCLE_ID}", cycleID+"");
			HttpPut req = new HttpPut(url);
			req.setEntity(payload);
			req.setHeader("Accept", "application/json");
			req.setHeader("Content-type", "application/json");
			response = restClient.getHttpclient().execute(req, restClient.getContext());

			int statusCode = response.getStatusLine().getStatusCode();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			restClient.destroy();
		}
		return map;
	}
	
	public static void main(String[] args) {
		RestClient rc = new RestClient("http://192.168.11.47", "test.manager", "test.manager");
		updateCycle(23L, 40, rc, "v3");
	}
}
