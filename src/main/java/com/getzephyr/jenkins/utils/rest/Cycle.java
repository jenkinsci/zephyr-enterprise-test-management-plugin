package com.getzephyr.jenkins.utils.rest;

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

public class Cycle {

	private static String URL_GET_CYCLES = "{SERVER}/flex/services/rest/latest/cycle";

	
	public static Long getCycleIdByCycleNameAndReleaseId(String cycleName, Long releaseId, String hostAddressWithProtocol, String userName, String password) {

		Long cycleId = 0L;

		HttpClientContext context = getClientContext(hostAddressWithProtocol, userName, password);
		HttpClient client = HttpClientBuilder.create().build();
		HttpResponse response = null;
		try {
			response = client.execute(new HttpGet(hostAddressWithProtocol + "/flex/services/rest/latest/cycle?name=" + URLEncoder.encode(cycleName, "utf-8") + "&releaseId=" + releaseId), context);
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
			
			
		} else {
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return cycleId;
	}
	
	public static Map<Long, String> getAllCyclesByReleaseID(Long releaseID, String hostAddressWithProtocol, String userName, String password) {


		Map<Long, String> cycles = new TreeMap<Long, String>();
		
		HttpClientContext context = getClientContext(hostAddressWithProtocol, userName, password);
		HttpClient client = HttpClientBuilder.create().build();
		HttpResponse response = null;
		
		final String url = URL_GET_CYCLES.replace("{SERVER}", hostAddressWithProtocol) + "?releaseId=" + releaseID;
		try {
			response = client.execute(new HttpGet(url), context);
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
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return cycles;
	}
	
	private static HttpClientContext getClientContext(String hostAddressWithProtocol, String userName, String password) {
		URL url;
		HttpClientContext context = null;
		try {
			url = new URL(hostAddressWithProtocol);
			HttpHost targetHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(userName, password));
			
			AuthCache authCache = new BasicAuthCache();
			authCache.put(targetHost, new BasicScheme());
			
			context = HttpClientContext.create();
			context.setCredentialsProvider(credsProvider);
			context.setAuthCache(authCache);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return context;
	}
}
