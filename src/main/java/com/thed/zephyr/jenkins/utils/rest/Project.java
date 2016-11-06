package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Project {

	private static String URL_GET_PROJECTS = "{SERVER}/flex/services/rest/{REST_VERSION}/project";
	
	public static void getProjectNameById(long id, RestClient restClient, String restVersion) {


		final String url = URL_GET_PROJECTS.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion) + "/" + id;

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

		} else {
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
	}
	
	public static Long getProjectIdByName(String projectName, RestClient restClient, String restVersion) {

		Long projectId = 0L;

		String url = null;
		try {
			url = URL_GET_PROJECTS.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion) + "?name=" + URLEncoder.encode(projectName, "utf-8");
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
				JSONArray projArray = new JSONArray(string);
				List<Long> projectIdList = new ArrayList<Long>();
				for(int i = 0; i < projArray.length(); i++) {
					Long id = projArray.getJSONObject(i).getLong("id");
					projectIdList.add(id);
				}
				
				Collections.sort(projectIdList);
				projectId = projectIdList.get(0);
				
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
	
		return projectId;
	}
	
	public static Map<Long, String> getAllProjects(RestClient restClient, String restVersion) {


		Map<Long, String> projects = new TreeMap<Long, String>();
		
		HttpResponse response = null;
		
		final String url = URL_GET_PROJECTS.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion) + "?status=2";
		try {
			response = restClient.getHttpclient().execute(new HttpGet(url), restClient.getContext());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (HttpHostConnectException e) {
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
				JSONArray projArray = new JSONArray(string);
				for(int i = 0; i < projArray.length(); i++) {
					
					
					JSONObject jsonObject = projArray.getJSONObject(i);
					JSONArray members = null;
					
					try {
						members = jsonObject.getJSONArray("members");
					} catch (Exception e) {
					} 
					
					if (members == null || members.length() == 0) {
						continue;
					}
					
					boolean isProjectAssignedToTheMember = false;
					for (int j = 0; j < members.length(); j++) {
						JSONObject member = members.getJSONObject(j);
						
						String user = member.getString("username");
						if(user.trim().equalsIgnoreCase(restClient.getUserName().trim())) {
							isProjectAssignedToTheMember = true;
							break;
						}
					}
					
					if(!isProjectAssignedToTheMember) {
						continue;
					}
					
					Long id = jsonObject.getLong("id");
					String projName = jsonObject.getString("name");
					projects.put(id, projName);
				}
				
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
		} else {
			
			projects.put(0L, "No Project");
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return projects;
	}
}
