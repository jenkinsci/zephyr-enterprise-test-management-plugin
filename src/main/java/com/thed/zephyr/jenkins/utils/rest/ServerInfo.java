package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class ServerInfo {

	private static String URL_GET_USERS = "{SERVER}/flex/services/rest/{REST_VERSION}/user/current";
	private static String URL_GET_ZEPHYR_VERSION = "{SERVER}/flex/version?format=xml";
	private static String INVALID_USER_CREDENTIALS = "Invalid user credentials";
	private static String INVALID_USER_ROLE = "User role should be manager or lead";
	private static String VALID_USER_ROLE = "User authentication is successful";

	public static boolean findServerAddressIsValidZephyrURL(RestClient restClient) {
		
        URL_GET_USERS = URL_GET_USERS.replace("{REST_VERSION}", "v1");

		return true;
	}

	public static Map<Boolean, String> validateCredentials(RestClient restClient, String restVersion) {

		Map<Boolean, String> statusMap = new HashMap<Boolean, String>();
		statusMap.put(false, INVALID_USER_CREDENTIALS);


		HttpResponse response = null;
		try {
			String constructedURL = URL_GET_USERS
					.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion);
			response = restClient.getHttpclient().execute(new HttpGet(constructedURL), restClient.getContext());
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

				JSONObject userObj = new JSONObject(string);

				if (userObj.getString("username").trim().equals(restClient.getUserName())) {
					String userRole = userObj.getJSONArray("roles")
							.getJSONObject(0).getString("name").trim();

					if (userRole.equalsIgnoreCase("manager")
							|| userRole.equalsIgnoreCase("lead")) {
						statusMap.clear();
						statusMap.put(true, VALID_USER_ROLE);
					} else {
						statusMap.clear();
						statusMap.put(false, INVALID_USER_ROLE);
						return statusMap;
					}
				}

			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (statusCode >= 401) {

			return statusMap;

		} else if (statusCode >= 400) {
			statusMap.clear();
			statusMap.put(false, INVALID_USER_ROLE);
			return statusMap;

		}
		return statusMap;

	}
	
	public static long getUserId(RestClient restClient, String restVersion) {


		long userId = 0;
		HttpResponse response = null;
		try {
			String constructedURL = URL_GET_USERS
					.replace("{SERVER}", restClient.getUrl()).replace("{REST_VERSION}", restVersion);
			response = restClient.getHttpclient().execute(new HttpGet(constructedURL), restClient.getContext());
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

				JSONObject userObj = new JSONObject(string);

				userId = userObj.getLong("id");
				
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return userId;

	}
}
