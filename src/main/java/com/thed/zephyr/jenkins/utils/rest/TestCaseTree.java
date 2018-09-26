package com.thed.zephyr.jenkins.utils.rest;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

public class TestCaseTree {

	private static String URL_GET_PHASES = "{SERVER}/flex/services/rest/{REST_VERSION}/testcasetree?type=Phase&revisionid=0&releaseid={VERSION_ID}";

	public static String getPhasesById(Long versionId, RestClient restClient, String restVersion) {

		String phaseInfo = null;
		HttpResponse response = null;

		try {
			final String url = URL_GET_PHASES.replace("{SERVER}", restClient.getUrl())
					.replace("{REST_VERSION}", restVersion).replace("{VERSION_ID}", versionId + "");
			response = restClient.getHttpclient().execute(new HttpGet(url), restClient.getContext());
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode >= 200 && statusCode < 300) {
				HttpEntity entity = response.getEntity();
				phaseInfo = EntityUtils.toString(entity);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Throwable e ) {
			e.printStackTrace();
		}

		return phaseInfo;
	}

	public static void main(String[] args) {
		RestClient rc = new RestClient("http://192.168.100.169:81", "test.manager", "test.manager");
		String phasesById = getPhasesById(5L, rc, "v3");
		
		System.out.println(phasesById);
	}
}
