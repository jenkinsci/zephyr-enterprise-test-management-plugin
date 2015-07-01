package com.thed.zephyr.jenkins.utils;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.thed.zephyr.jenkins.utils.rest.ServerInfo;

public class ConfigurationValidator {

	public static boolean validateZephyrConfiguration(String serverAddress,
			String username, String password) {

		boolean status = false;
		if (StringUtils.isBlank(serverAddress)) {
			return status;
		}

		if (StringUtils.isBlank(username)) {
			return status;
		}

		if (StringUtils.isBlank(password)) {
			return status;
		}

		if (!(serverAddress.trim().startsWith("https://") || serverAddress
				.trim().startsWith("http://"))) {
			return status;
		}

		String zephyrURL = URLValidator.validateURL(serverAddress);

		if (!zephyrURL.startsWith("http")) {
			return status;
		}

		if (!ServerInfo.findServerAddressIsValidZephyrURL(zephyrURL)) {
			return status;
		}

		Map<Boolean, String> credentialValidationResultMap = ServerInfo
				.validateCredentials(zephyrURL, username, password);
		if (credentialValidationResultMap.containsKey(false)) {
			return status;
		}

		status = true;
		return status;
	}
}
