package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Release {

	private static String URL_GET_RELEASES = "{SERVER}/flex/services/rest/latest/release";

	
	public static Long getReleaseIdByNameProjectId(String releaseName, Long projectId, RestClient restClient) {

		Long releaseId = 0L;

		HttpResponse response = null;
		try {
			response = restClient.getHttpclient().execute(new HttpGet(restClient.getUrl() + "/flex/services/rest/latest/release?name=" + URLEncoder.encode(releaseName, "utf-8") + "&project.id=" + projectId), restClient.getContext());
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
				JSONArray releaseArray = new JSONArray(string);
				List<Long> releaseIdList = new ArrayList<Long>();
				for(int i = 0; i < releaseArray.length(); i++) {
					Long id = releaseArray.getJSONObject(i).getLong("id");
					releaseIdList.add(id);
				}
				
				Collections.sort(releaseIdList);
				releaseId = releaseIdList.get(0);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
		} else {
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return releaseId;
	}
	
	public static Map<Long, String> getAllReleasesByProjectID(Long projectID, RestClient restClient) {


		Map<Long, String> releases = new TreeMap<Long, String>();
		
		HttpResponse response = null;
		
		final String url = URL_GET_RELEASES.replace("{SERVER}", restClient.getUrl()) + "?project.id=" + projectID;
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
				JSONArray releasesArray = new JSONArray(string);
				for(int i = 0; i < releasesArray.length(); i++) {
					JSONObject releaseObject = releasesArray.getJSONObject(i);
					
					int visibility = releaseObject.getInt("status");
					if (visibility == 1) {
						continue;
					}
					Long id = releaseObject.getLong("id");
					String projName = releaseObject.getString("name");
					releases.put(id, projName);
				}
				
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
		} else {
			
			releases.put(0L, "No Release");
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return releases;
	}
	
}
