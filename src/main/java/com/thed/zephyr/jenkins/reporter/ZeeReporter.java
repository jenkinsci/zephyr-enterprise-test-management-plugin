package com.thed.zephyr.jenkins.reporter;

/**
 * @author mohan
 */

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.thed.service.soap.RemoteTestcase;
import com.thed.zephyr.jenkins.model.TestCaseResultModel;
import com.thed.zephyr.jenkins.model.ZephyrConfigModel;
import com.thed.zephyr.jenkins.model.ZephyrInstance;
import com.thed.zephyr.jenkins.utils.URLValidator;
import com.thed.zephyr.jenkins.utils.ZephyrSoapClient;
import com.thed.zephyr.jenkins.utils.rest.Cycle;
import com.thed.zephyr.jenkins.utils.rest.Project;
import com.thed.zephyr.jenkins.utils.rest.Release;
import com.thed.zephyr.jenkins.utils.rest.ServerInfo;

public class ZeeReporter extends Notifier {

	private String projectKey;
	private String releaseKey;
	private String cycleKey;;
	private String cyclePrefix;
	private String serverAddress;
	private String username;
	private String password;
	private String cycleDuration;

	private Long projectId;
	private Long releaseId;
	private Long cycleId;

	private ZephyrConfigModel zephyrData;

	private boolean debugFlag;
	private boolean createPackage;

	private FilePath workspace;

	private static final String PluginName = new String("[ZephyrTestResultReporter]");
	private final String pInfo = String.format("%s [INFO]", PluginName);

	public static PrintStream logger;

	@DataBoundConstructor
	public ZeeReporter(String serverAddress, String projectKey,
			String releaseKey, String cycleKey, String cyclePrefix,
			String cycleDuration, boolean createPackage) {
		this.serverAddress = serverAddress;
		this.projectKey = projectKey;
		this.releaseKey = releaseKey;
		this.cycleKey = cycleKey;
		this.cyclePrefix = cyclePrefix;
		this.createPackage = createPackage;
		this.cycleDuration = cycleDuration;
	}

	// @Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(final AbstractBuild build, final Launcher launcher,
			final BuildListener listener) {
		logger = listener.getLogger();
		logger.printf("%s Examining test results...%n", pInfo);
		debugLog(listener, String.format("Build result is %s%n", build
				.getResult().toString()));

		if (StringUtils.isBlank(serverAddress)
				|| StringUtils.isBlank(projectKey)
				|| StringUtils.isBlank(releaseKey)
				|| StringUtils.isBlank(cycleKey)
				|| ZephyrConstants.ADD_ZEPHYR_GLOBAL_CONFIG.equals(serverAddress.trim())
				|| ZephyrConstants.ADD_ZEPHYR_GLOBAL_CONFIG.equals(projectKey.trim())
				|| ZephyrConstants.ADD_ZEPHYR_GLOBAL_CONFIG.equals(releaseKey.trim())
				|| ZephyrConstants.ADD_ZEPHYR_GLOBAL_CONFIG.equals(cycleKey.trim())) {

			logger.println("Cannot Proceed");
			return false;
		}

		List<ZephyrInstance> tempZephyrInstances = getDescriptor().getZephyrInstances();

		for (ZephyrInstance tZI : tempZephyrInstances) {

			if (StringUtils.isNotBlank(tZI.getServerAddress()) && tZI.getServerAddress().trim().equals(serverAddress)) {
				username = tZI.getUsername();
				password = tZI.getPassword();
				break;
			}

		}

		initializeZephyrData();


		TestResultAction testResultAction = build.getAction(TestResultAction.class);
		Collection<SuiteResult> suites = testResultAction.getResult().getSuites();

		if (suites == null) {
			logger.println("Problem parsing JUnit test Results.");
			logger.println("Problem parsing JUnit test Results.");
			return false;
		}

		Map<String, Boolean> zephyrTestCaseMap = new HashMap<String, Boolean>();
		Set<String> packageNames = new HashSet<String>();

		prepareTestResults(suites, zephyrTestCaseMap, packageNames);

		logger.print("Total Test Cases : " + zephyrTestCaseMap.size());
		ZephyrSoapClient client = new ZephyrSoapClient();
		List<TestCaseResultModel> testcases = new ArrayList<TestCaseResultModel>();

		Set<String> keySet = zephyrTestCaseMap.keySet();

		for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
			String testCaseName = iterator.next();
			Boolean isPassed = zephyrTestCaseMap.get(testCaseName);
			RemoteTestcase testcase = new RemoteTestcase();
			testcase.setName(testCaseName);
			testcase.setComments("Created via Jenkins Zephyr Plugin!");
			testcase.setAutomated(false);
			testcase.setExternalId("99999");
			testcase.setPriority("1");
			testcase.setTag("API");

			TestCaseResultModel caseWithStatus = new TestCaseResultModel();
			caseWithStatus.setPassed(isPassed);
			caseWithStatus.setRemoteTestcase(testcase);
			testcases.add(caseWithStatus);
		}

		try {
			zephyrData.setTestcases(testcases);
			zephyrData.setPackageNames(packageNames);
			zephyrData.setCreatePackage(createPackage);
			client.manageTestResults1(zephyrData);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}

		// }
		logger.printf("%s Done.%n", pInfo);
		return true;
	}

	/**
	 * @param suites
	 * @param zephyrTestCaseMap
	 * @param packageNames
	 */
	private void prepareTestResults(Collection<SuiteResult> suites,
			Map<String, Boolean> zephyrTestCaseMap, Set<String> packageNames) {
		for (Iterator<SuiteResult> iterator = suites.iterator(); iterator
				.hasNext();) {
			SuiteResult suiteResult = iterator.next();
			List<CaseResult> cases = suiteResult.getCases();
			for (CaseResult caseResult : cases) {
				boolean isPassed = caseResult.isPassed();
				String name = caseResult.getFullName();
				if (!zephyrTestCaseMap.containsKey(name)) {
					zephyrTestCaseMap.put(name, isPassed);
					packageNames.add(caseResult.getPackageName());
				}
			}
		}
	}

	private void determineUserId(String url, String usr, String pass) {
		long userId = ServerInfo.getUserId(url, usr, pass);
		zephyrData.setUserId(userId);
	}

	private void determineCyclePrefix() {
		if (StringUtils.isNotBlank(cyclePrefix)) {
			zephyrData.setCyclePrefix(cyclePrefix + "_");
		} else {
			zephyrData.setCyclePrefix("Automation_");
		}
	}

	private void initializeZephyrData() {
		zephyrData = new ZephyrConfigModel();
		zephyrData.setUserName(username);
		zephyrData.setPassword(password);
		
		String hostName = URLValidator.fetchURL(serverAddress);
		zephyrData.setZephyrURL(hostName);
		zephyrData.setCycleDuration(cycleDuration);
		determineProjectID(hostName);
		determineReleaseID(hostName);
		determineCycleID(hostName);
		determineCyclePrefix();
		determineUserId(hostName, username, password);

	}

	private void determineCycleID(String hostName) {

		if (cycleKey.equalsIgnoreCase(ZephyrConstants.NEW_CYCLE_KEY)) {
			zephyrData.setCycleId(ZephyrConstants.NEW_CYCLE_KEY_IDENTIFIER);
			return;
		}
		try {
			cycleId = Long.parseLong(cycleKey);
		} catch (NumberFormatException e1) {
			logger.println("Cycle Key appears to be the name of the cycle");
			try {
				Long cycleIdByCycleNameAndReleaseId = Cycle
						.getCycleIdByCycleNameAndReleaseId(cycleKey, releaseId,
								hostName, username, password);
				cycleId = cycleIdByCycleNameAndReleaseId;
			} catch (Exception e) {
				e.printStackTrace();
			}
			e1.printStackTrace();
		}

		zephyrData.setCycleId(cycleId);
	}

	private void determineReleaseID(String hostName) {

		try {
			releaseId = Long.parseLong(releaseKey);
		} catch (NumberFormatException e1) {
			logger.println("Release Key appears to be Name of the Release");
			try {
				Long releaseIdByReleaseNameProjectId = Release
						.getReleaseIdByNameProjectId(releaseKey, projectId,
								hostName, username, password);
				releaseId = releaseIdByReleaseNameProjectId;
			} catch (Exception e) {
				e.printStackTrace();
			}
			e1.printStackTrace();
		}
		zephyrData.setReleaseId(releaseId);
	}

	private void determineProjectID(String hostName) {
		try {
			projectId = Long.parseLong(projectKey);
		} catch (NumberFormatException e1) {
			logger.println("Project Key appears to be Name of the project");
			try {
				Long projectIdByName = Project.getProjectIdByName(projectKey,
						hostName, username, password);
				projectId = projectIdByName;
			} catch (Exception e) {
				e.printStackTrace();
			}
			e1.printStackTrace();
		}

		zephyrData.setZephyrProjectId(projectId);
	}

	void debugLog(final BuildListener listener, final String message) {
		if (!this.debugFlag) {
			return;
		}
		PrintStream logger = listener.getLogger();
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getReleaseKey() {
		return releaseKey;
	}

	public void setReleaseKey(String releaseKey) {
		this.releaseKey = releaseKey;
	}

	public String getCycleKey() {
		return cycleKey;
	}

	public void setCycleKey(String cycleKey) {
		this.cycleKey = cycleKey;
	}

	public String getCyclePrefix() {
		return cyclePrefix;
	}

	public void setCyclePrefix(String cyclePrefix) {
		this.cyclePrefix = cyclePrefix;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCycleDuration() {
		return cycleDuration;
	}

	public void setCycleDuration(String cycleDuration) {
		this.cycleDuration = cycleDuration;
	}
}
