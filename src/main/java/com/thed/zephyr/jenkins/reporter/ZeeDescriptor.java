package com.thed.zephyr.jenkins.reporter;

import static com.thed.zephyr.jenkins.reporter.ZeeConstants.ADD_ZEPHYR_GLOBAL_CONFIG;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_DURATION_1_DAY;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_DURATION_30_DAYS;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_DURATION_7_DAYS;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NAME_POST_BUILD_ACTION;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.thed.zephyr.jenkins.model.ZephyrConfigModel;
import com.thed.zephyr.jenkins.model.ZephyrInstance;
import com.thed.zephyr.jenkins.utils.ConfigurationValidator;
import com.thed.zephyr.jenkins.utils.URLValidator;
import com.thed.zephyr.jenkins.utils.ZephyrSoapClient;
import com.thed.zephyr.jenkins.utils.rest.Cycle;
import com.thed.zephyr.jenkins.utils.rest.Project;
import com.thed.zephyr.jenkins.utils.rest.Release;
import com.thed.zephyr.jenkins.utils.rest.RestClient;
import com.thed.zephyr.jenkins.utils.rest.ServerInfo;

@Extension
public final class ZeeDescriptor extends BuildStepDescriptor<Publisher> {

	private List<ZephyrInstance> zephyrInstances;
	Map<Long, String> projects;
	Map<Long, String> releases;
	Map<Long, String> cycles;
	private String tempServerAddress;
	private String tempUserName;
	private String tempPassword;
	private RestClient restClient;

	public List<ZephyrInstance> getZephyrInstances() {
		return zephyrInstances;
	}

	public void setZephyrInstances(List<ZephyrInstance> zephyrInstances) {
		this.zephyrInstances = zephyrInstances;
	}

	public ZeeDescriptor() {
		super(ZeeReporter.class);
		load();
	}

	@Override
	public Publisher newInstance(StaplerRequest req, JSONObject formData)
			throws FormException {
		return super.newInstance(req, formData);
	}

	@Override
	public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
		return true;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData)
			throws FormException {
		req.bindParameters(this);
		this.zephyrInstances = new ArrayList<ZephyrInstance>();
		Object object = formData.get("zephyrInstances");
		if (object instanceof JSONArray) {
			JSONArray jArr = (JSONArray) object;
			for (Iterator iterator = jArr.iterator(); iterator.hasNext();) {
				JSONObject jObj = (JSONObject) iterator.next();
				ZephyrInstance zephyrInstance = new ZephyrInstance();

				String server = jObj.getString("serverAddress").trim();
				String user = jObj.getString("username").trim();
				String pass = jObj.getString("password").trim();

				zephyrInstance.setServerAddress(server);
				zephyrInstance.setUsername(user);
				zephyrInstance.setPassword(pass);
				
				RestClient restClient = new RestClient(URLValidator.validateURL(server), user, pass);

				boolean zephyrServerValidation = ConfigurationValidator
						.validateZephyrConfiguration(restClient);
				if (zephyrServerValidation) {
					this.zephyrInstances.add(zephyrInstance);
				}
			}

		} else if (object instanceof JSONObject) {
			JSONObject jObj = formData.getJSONObject("zephyrInstances");
			ZephyrInstance zephyrInstance = new ZephyrInstance();

			String server = jObj.getString("serverAddress").trim();
			String user = jObj.getString("username").trim();
			String pass = jObj.getString("password").trim();

			zephyrInstance.setServerAddress(server);
			zephyrInstance.setUsername(user);
			zephyrInstance.setPassword(pass);

			RestClient restClient = new RestClient(URLValidator.validateURL(server), user, pass);

			boolean zephyrServerValidation = ConfigurationValidator
					.validateZephyrConfiguration(restClient);
			if (zephyrServerValidation) {
				this.zephyrInstances.add(zephyrInstance);
			}

		}
		save();
		closeHTTPClient();
		return super.configure(req, formData);
	}

	/**
	 * 
	 */
	private void closeHTTPClient() {
		try {
			restClient.getHttpclient().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getDisplayName() {
		return NAME_POST_BUILD_ACTION;
	}

	public FormValidation doCheckProjectKey(@QueryParameter String value) {
		if (value.isEmpty()) {
			return FormValidation.error("You must provide a project key.");
		} else {
			return FormValidation.ok();
		}
	}

	public FormValidation doTestConnection(
			@QueryParameter String serverAddress,
			@QueryParameter String username, @QueryParameter String password) {


		if (StringUtils.isBlank(serverAddress)) {
			return FormValidation.error("Please enter the server name");
		}

		if (StringUtils.isBlank(username)) {
			return FormValidation.error("Please enter the username");
		}

		if (StringUtils.isBlank(password)) {
			return FormValidation.error("Please enter the password");
		}

		if (!(serverAddress.trim().startsWith("https://") || serverAddress
				.trim().startsWith("http://"))) {
			return FormValidation.error("Incorrect server address format");
		}

		String zephyrURL = URLValidator.validateURL(serverAddress);
		RestClient restClient = new RestClient(zephyrURL, username, password);

		if (!zephyrURL.startsWith("http")) {
			return FormValidation.error(zephyrURL);
		}

		if (!ServerInfo.findServerAddressIsValidZephyrURL(restClient)) {
			return FormValidation.error("This is not a valid Zephyr Server");
		}

		Map<Boolean, String> credentialValidationResultMap = ServerInfo
				.validateCredentials(restClient);
		if (credentialValidationResultMap.containsKey(false)) {
			return FormValidation.error(credentialValidationResultMap
					.get(false));
		}

		return FormValidation.ok("Connection to Zephyr has been validated");
	}

	public ListBoxModel doFillServerAddressItems(
			@QueryParameter String serverAddress) {

		ListBoxModel m = new ListBoxModel();

		if (this.zephyrInstances.size() > 0) {

			for (ZephyrInstance s : this.zephyrInstances) {
				m.add(s.getServerAddress());
			}
		} else if (StringUtils.isBlank(serverAddress)
				|| serverAddress.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
		} else {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
		}
		return m;
	}

	public ListBoxModel doFillProjectKeyItems(
			@QueryParameter String serverAddress) {
		ListBoxModel m = new ListBoxModel();

		tempServerAddress = URLValidator.validateURL(serverAddress);

		if (StringUtils.isBlank(serverAddress)
				|| serverAddress.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)
				|| (this.zephyrInstances.size() == 0)) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return m;
		}

		setCredentials(serverAddress);
		
		restClient = new RestClient(tempServerAddress, tempUserName, tempPassword);

		projects = Project.getAllProjects(restClient);
		Set<Entry<Long, String>> projectEntrySet = projects.entrySet();

		for (Iterator<Entry<Long, String>> iterator = projectEntrySet
				.iterator(); iterator.hasNext();) {
			Entry<Long, String> entry = iterator.next();
			m.add(entry.getValue(), entry.getKey() + "");
		}

		return m;
	}

	/**
	 * @param serverAddress
	 */
	private void setCredentials(String serverAddress) {
		for (ZephyrInstance z : zephyrInstances) {
			if (z.getServerAddress().trim().equals(serverAddress)) {
				tempUserName = z.getUsername();
				tempPassword = z.getPassword();
			}
		}
	}

	public ListBoxModel doFillReleaseKeyItems(
			@QueryParameter String projectKey,
			@QueryParameter String serverAddress) {
		setCredentials(serverAddress);

		ListBoxModel m = new ListBoxModel();

		if (StringUtils.isBlank(projectKey)
				|| projectKey.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)
				|| (this.zephyrInstances.size() == 0)) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return m;
		}

		long parseLong = Long.parseLong(projectKey);
		if(restClient == null ) {
			restClient = new RestClient(serverAddress, tempPassword, tempPassword);
		}
		releases = Release.getAllReleasesByProjectID(parseLong,restClient);
		Set<Entry<Long, String>> releaseEntrySet = releases.entrySet();

		for (Iterator<Entry<Long, String>> iterator = releaseEntrySet
				.iterator(); iterator.hasNext();) {
			Entry<Long, String> entry = iterator.next();
			m.add(entry.getValue(), entry.getKey() + "");
		}

		return m;

	}

	public ListBoxModel doFillCycleKeyItems(@QueryParameter String releaseKey,
			@QueryParameter String serverAddress) {
		setCredentials(serverAddress);

		ListBoxModel m = new ListBoxModel();

		if (StringUtils.isBlank(releaseKey)
				|| releaseKey.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)
				|| (this.zephyrInstances.size() == 0)) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return m;
		}
		
		if(restClient == null ) {
			restClient = new RestClient(serverAddress, tempPassword, tempPassword);
		}

		long parseLong = Long.parseLong(releaseKey);

		cycles = Cycle.getAllCyclesByReleaseID(parseLong, restClient);
		Set<Entry<Long, String>> releaseEntrySet = cycles.entrySet();

		for (Iterator<Entry<Long, String>> iterator = releaseEntrySet
				.iterator(); iterator.hasNext();) {
			Entry<Long, String> entry = iterator.next();
			m.add(entry.getValue(), entry.getKey() + "");
		}

		m.add("New Cycle", NEW_CYCLE_KEY);

		return m;
	}

	public ListBoxModel doFillCycleDurationItems(
			@QueryParameter String serverAddress,
			@QueryParameter String projectKey) {
		setCredentials(serverAddress);

		long zephyrProjectId = Long.parseLong(projectKey);
		ZephyrConfigModel zephyrData = new ZephyrConfigModel();
		zephyrData.setZephyrProjectId(zephyrProjectId);
		ListBoxModel m = new ListBoxModel();
		int fetchProjectDuration = 1;
		
		if(restClient == null ) {
			restClient = new RestClient(serverAddress, tempPassword, tempPassword);
		}
		zephyrData.setRestClient(restClient);
		try {
			fetchProjectDuration = ZephyrSoapClient
					.fetchProjectDuration(zephyrData);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}

		if (fetchProjectDuration == -1) {
			m.add(CYCLE_DURATION_30_DAYS);
			m.add(CYCLE_DURATION_7_DAYS);
			m.add(CYCLE_DURATION_1_DAY);
			return m;
		}

		if (fetchProjectDuration >= 29) {
			m.add(CYCLE_DURATION_30_DAYS);
		}

		if (fetchProjectDuration >= 6) {
			m.add(CYCLE_DURATION_7_DAYS);
		}
		m.add(CYCLE_DURATION_1_DAY);
		return m;
	}
}