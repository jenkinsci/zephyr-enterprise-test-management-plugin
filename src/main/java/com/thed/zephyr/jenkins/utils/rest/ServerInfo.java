package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class ServerInfo {

	private static String URL_GET_PROJECTS = "{SERVER}/flex/services/rest/latest/info";
	private static String URL_GET_USERS = "{SERVER}/flex/services/rest/latest/user/current";
	private static String USER_NOT_FOUND_MSG = "errorCode";
	private static String INVALID_USER_CREDENTIALS = "Invalid user credentials";
	private static String INVALID_USER_ROLE = "User role should be manager or lead";
	private static String VALID_USER_ROLE = "User authentication is successful";

	public static boolean findServerAddressIsValidZephyrURL(
			String hostNameWithProtocol) {
		boolean status = false;
		CloseableHttpClient httpclient = null;
		try {
			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					builder.build(),
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			httpclient = HttpClients.custom().setSSLSocketFactory(sslsf)
					.build();
		} catch (KeyManagementException e1) {
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (KeyStoreException e1) {
			e1.printStackTrace();
		}

		if (httpclient == null) {
			return status;
		}
		HttpClientContext context = getClientContext(hostNameWithProtocol,
				"test.manager", "test.maager");
		HttpResponse response = null;
		try {
			String constructedURL = URL_GET_PROJECTS.replace("{SERVER}",
					hostNameWithProtocol);

			response = httpclient.execute(new HttpGet(constructedURL));
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

		} else if (statusCode >= 400) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity, "utf-8");
				if (string.contains(USER_NOT_FOUND_MSG))
					status = true;
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				throw new ClientProtocolException(
						"Unexpected response status: " + statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
		return status;

	}

	private static HttpClientContext getClientContext(
			String hostAddressWithProtocol, String userName, String password) {
		URL url;
		HttpClientContext context = null;
		try {
			url = new URL(hostAddressWithProtocol);
			HttpHost targetHost = new HttpHost(url.getHost(), url.getPort(),
					url.getProtocol());
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

	public static Map<Boolean, String> validateCredentials(String zephyrURL,
			String username, String password) {

		Map<Boolean, String> statusMap = new HashMap<Boolean, String>();
		statusMap.put(false, INVALID_USER_CREDENTIALS);

		CloseableHttpClient httpclient = null;
		try {
			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					builder.build(),
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			httpclient = HttpClients.custom().setSSLSocketFactory(sslsf)
					.build();
		} catch (KeyManagementException e1) {
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (KeyStoreException e1) {
			e1.printStackTrace();
		}

		HttpClientContext context = getClientContext(zephyrURL, username,
				password);
		HttpResponse response = null;
		try {
			String constructedURL = URL_GET_USERS
					.replace("{SERVER}", zephyrURL);
			response = httpclient.execute(new HttpGet(constructedURL), context);
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

				if (userObj.getString("username").trim().equals(username)) {
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

		} else {
			try {
				throw new ClientProtocolException(
						"Unexpected response status: " + statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
		return statusMap;

	}
	
	public static long getUserId(String zephyrURL,
			String username, String password) {


		long userId = 0;
		CloseableHttpClient httpclient = null;
		try {
			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					builder.build(),
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			httpclient = HttpClients.custom().setSSLSocketFactory(sslsf)
					.build();
		} catch (KeyManagementException e1) {
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (KeyStoreException e1) {
			e1.printStackTrace();
		}

		HttpClientContext context = getClientContext(zephyrURL, username,
				password);
		HttpResponse response = null;
		try {
			String constructedURL = URL_GET_USERS
					.replace("{SERVER}", zephyrURL);
			response = httpclient.execute(new HttpGet(constructedURL), context);
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

		} else {
			try {
				throw new ClientProtocolException(
						"Unexpected response status: " + statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
		return userId;

	}
	
	public static void main(String[] args) {
		System.out.println(getUserId("http://localhost", "test.lead", "test.lead"));
	}
}
