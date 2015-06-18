package com.getzephyr.jenkins.utils.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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

import com.getzephyr.jenkins.model.TestCaseModel;

public class TestCaseUtil {

	private static final String URL_GET_TEST_CASE_DETAILS = "{SERVER}/flex/services/rest/latest/testcase";
	
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
	
	
	public static List<TestCaseModel> getTestCaseDetails(String testCaseName, String hostAddressWithProtocol, String userName, String password) {


		List<TestCaseModel> testCaseDetails = null;
		HttpClientContext context = getClientContext(hostAddressWithProtocol, userName, password);
		HttpClient client = HttpClientBuilder.create().build();
		HttpResponse response = null;
		
		String url = null;
		try {
			url = URL_GET_TEST_CASE_DETAILS.replace("{SERVER}", hostAddressWithProtocol) + "?testcase.name=" + URLEncoder.encode(testCaseName, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
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
				testCaseDetails = new ArrayList<TestCaseModel>();

				JSONArray projArray = new JSONArray(string);
				for(int i = 0; i < projArray.length(); i++) {
					String name = projArray.getJSONObject(i).getJSONObject("testcase").getString("name");
					Long testCaseId = projArray.getJSONObject(i).getJSONObject("testcase").getLong("id");
					Long phaseTestCaseId = projArray.getJSONObject(i).getLong("id");
					Long remoteRepositoryId = projArray.getJSONObject(i).getLong("remoteRepositoryId");
					
					TestCaseModel testCaseModel = new TestCaseModel();
					testCaseModel.setName(name);
					testCaseModel.setTestCaseId(testCaseId);
					testCaseModel.setPhaseTestCaseId(phaseTestCaseId);
					testCaseModel.setRemoteRepositoryId(remoteRepositoryId);
					
					testCaseDetails.add(testCaseModel);
				}
				
				
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
	
		return testCaseDetails;
	}
}
