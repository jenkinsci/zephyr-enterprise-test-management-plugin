package com.thed.zephyr.jenkins.reporter;

/**
 * @author mohan
 */

import static com.thed.zephyr.jenkins.reporter.ZeeConstants.ADD_ZEPHYR_GLOBAL_CONFIG;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.AUTOMATED;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_PREFIX_DEFAULT;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.EXTERNAL_ID;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY_IDENTIFIER;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.TEST_CASE_COMMENT;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.TEST_CASE_PRIORITY;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.TEST_CASE_TAG;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.junit.*;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
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
import com.thed.zephyr.jenkins.utils.rest.RestClient;
import com.thed.zephyr.jenkins.utils.rest.ServerInfo;

public class ZeeReporter extends Notifier {

	private String projectKey;
	private String releaseKey;
	private String cycleKey;;
	private String cyclePrefix;
	private String serverAddress;
	private String cycleDuration;
	private boolean createPackage;


	public static PrintStream logger;
	private static final String PluginName = "[Zephyr Enterprise Test Management";
    private static final String JUNIT_PFX = "TEST-*";
    private static final String SUREFIRE_REPORT = "surefire-reports";
    private static final String JUNIT_SFX = "/*.xml";
	private final String pInfo = String.format("%s [INFO]", PluginName);


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

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(final AbstractBuild build, final Launcher launcher,
			final BuildListener listener) throws IOException, InterruptedException {
		logger = listener.getLogger();
		logger.printf("%s Examining test results...%n", pInfo);

		if (!validateBuildConfig()) {
			logger.println("Cannot Proceed. Please verify the job configuration");
			return false;
		}

		int number = build.getRootBuild().getNumber();
		ZephyrConfigModel zephyrConfig = initializeZephyrData();
		ZephyrSoapClient client = new ZephyrSoapClient();
		zephyrConfig.setBuilNumber(number);
		boolean prepareZephyrTests = prepareZephyrTests(listener, launcher, build, zephyrConfig);
		
    	if(!prepareZephyrTests) {
			logger.println("Error parsing surefire reports.");
			logger.println("Please ensure \"Publish JUnit test result report is added\" as a post build action");
			return false;
    	}


		try {
			client.uploadTestResults(zephyrConfig);
		} catch (DatatypeConfigurationException e) {
			logger.printf("Error uploading test results to Zephyr");
		}

		logger.printf("%s Done uploading tests to Zephyr.%n", pInfo);
		return true;
	}

	/**
	 * Fetches the credentials from the global configuration and creates a restClient
	 * @return RestClient
	 */
	private RestClient buildRestClient(ZephyrConfigModel zephyrData) {
		List<ZephyrInstance> zephyrServers = getDescriptor().getZephyrInstances();

		for (ZephyrInstance zephyrServer : zephyrServers) {
			if (StringUtils.isNotBlank(zephyrServer.getServerAddress()) && zephyrServer.getServerAddress().trim().equals(serverAddress)) {
				zephyrData.setSelectedZephyrServer(zephyrServer);
				RestClient restClient = new RestClient(zephyrServer);
				return restClient;
			}
		}
		return null;
	}

    /**
     * Collects the surefire results and prepares Zephyr Tests
     * @param build
     * @param zephyrConfig
     * @return
     */
    private boolean prepareZephyrTests(BuildListener listener, Launcher launcher, final AbstractBuild build,
                                       ZephyrConfigModel zephyrConfig) throws IOException, InterruptedException {

        boolean status = true;
        Collection<SuiteResult> suites = new ArrayList<>();

        boolean isMavenProject = build.getProject().getClass().getName().toLowerCase().contains("maven");
        TestResultAction testResultAction = null;
        if (isMavenProject) {
        	AggregatedTestResultAction testResultAction1 = build.getAction(AggregatedTestResultAction.class);
            try {
                List<ChildReport> result = testResultAction1.getChildReports();
                
                for (ChildReport childReport : result) {
                	if (childReport.result instanceof TestResult) {
                		Collection<SuiteResult> str = ((TestResult) childReport.result).getSuites();
                		suites.addAll(str);
                	}
				}
            } catch (Exception e) {
                logger.println(e.getMessage());
                return false;
            }

            if (suites == null || suites.size() == 0) {
                return false;
            }

//            String basedDir = build.getWorkspace().toURI().getPath();
//            List<String> resultFolders = scanJunitTestResultFolder(basedDir);
//
//            if (resultFolders == null || resultFolders.size() == 0) {
//                return false;
//            }
//            for (String res : resultFolders) {
//                if (res.contains(SUREFIRE_REPORT)) {
//                    try {
//                        List<TestResult> results = parse(listener, launcher, build, res + JUNIT_SFX);
//                        for (TestResult testResult : results) {
//                            suites.addAll(testResult.getSuites());
//                        }
//                    } catch (Exception e) {
//                        logger.print("Could not parse results from surefire reports" + e.getMessage());
//                    }
//                }
//            }
        } else {
            testResultAction = build.getAction(TestResultAction.class);
            try {
                suites = testResultAction.getResult().getSuites();
            } catch (Exception e) {
                logger.println(e.getMessage());
                return false;
            }

            if (suites == null || suites.size() == 0) {
                return false;
            }
        }

		Set<String> packageNames = new HashSet<String>();

		Map<String, Boolean> zephyrTestCaseMap = prepareTestResults(suites, packageNames);

		logger.print("Total Test Cases : " + zephyrTestCaseMap.size());
		
		List<TestCaseResultModel> testcases = new ArrayList<TestCaseResultModel>();
		Set<String> keySet = zephyrTestCaseMap.keySet();

		for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
			String testCaseName = iterator.next();
			Boolean isPassed = zephyrTestCaseMap.get(testCaseName);
			RemoteTestcase testcase = new RemoteTestcase();
			testcase.setName(testCaseName);
			testcase.setComments(TEST_CASE_COMMENT);
			testcase.setAutomated(AUTOMATED);
			testcase.setExternalId(EXTERNAL_ID);
			testcase.setPriority(TEST_CASE_PRIORITY);
			testcase.setTag(TEST_CASE_TAG);

			TestCaseResultModel caseWithStatus = new TestCaseResultModel();
			caseWithStatus.setPassed(isPassed);
			caseWithStatus.setRemoteTestcase(testcase);
			testcases.add(caseWithStatus);
		}

		zephyrConfig.setTestcases(testcases);
		zephyrConfig.setPackageNames(packageNames);
		zephyrConfig.setCreatePackage(createPackage);
		
		return status;
	}


    private List<TestResult> parse(BuildListener listener,
                                   Launcher launcher, AbstractBuild build, String testResultLocation) throws Exception {
        JUnitParser jUnitParser = new JUnitParser(true);
        List<TestResult> testResults = new ArrayList<>();
        testResults.add(jUnitParser.parseResult(testResultLocation, build, build.getWorkspace(), launcher, listener));
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(build.getStartTimeInMillis());
        return testResults;
    }

    private static List<String> scanJunitTestResultFolder(String basedDir) {
        File currentBasedDir = new File(basedDir);
        FileSet fs = Util.createFileSet(new File(basedDir), JUNIT_PFX);
        DirectoryScanner ds = fs.getDirectoryScanner();
        List<String> resultFolders = new ArrayList<>();
        //if based dir match junit file, we add based dir
        if (ds.getIncludedFiles().length > 0)
            resultFolders.add("");

        for (String notIncludedDirName : ds.getNotIncludedDirectories()) {
            if (!StringUtils.isEmpty(notIncludedDirName)) {
                File dirToScan = new File(currentBasedDir.getPath(), notIncludedDirName);
                FileSet subFileSet = Util.createFileSet(dirToScan, JUNIT_PFX);
                DirectoryScanner subDirScanner = subFileSet.getDirectoryScanner();
                if (subDirScanner.getIncludedFiles().length > 0) {
                    resultFolders.add(notIncludedDirName);
                }
            }
        }
        return resultFolders;
    }

	/**
	 * Validates the build configuration details
	 */
	private boolean validateBuildConfig() {
		boolean valid = true;
		if (StringUtils.isBlank(serverAddress)
				|| StringUtils.isBlank(projectKey)
				|| StringUtils.isBlank(releaseKey)
				|| StringUtils.isBlank(cycleKey)
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(serverAddress.trim())
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(projectKey.trim())
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(releaseKey.trim())
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(cycleKey.trim())) {
			valid = false;
		}
		return valid;
	}

	/**
	 * Collects Surefire test results
	 * @param suites
	 * @param packageNames
	 * @return
	 */
	private Map<String, Boolean> prepareTestResults(Collection<SuiteResult> suites,	Set<String> packageNames) {
		Map<String,Boolean> zephyrTestCaseMap = new HashMap<String, Boolean>();
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
		return zephyrTestCaseMap;
	}

	/**
	 *
	 * @return ZephyrConfigModel
	 */
	private ZephyrConfigModel initializeZephyrData() {
		ZephyrConfigModel zephyrData = new ZephyrConfigModel();
		
		RestClient restClient = buildRestClient(zephyrData);
		try {
			zephyrData.setCycleDuration(cycleDuration);
			determineProjectID(zephyrData, restClient);
			determineReleaseID(zephyrData, restClient);
			determineCycleID(zephyrData, restClient);
			determineCyclePrefix(zephyrData);
			determineUserId(zephyrData, restClient);
		}finally {
			restClient.destroy();
		}
	
		return zephyrData;
	}

	/**
	 *
	 * @param zephyrData
	 * @param restClient
	 */
	private void determineUserId(ZephyrConfigModel zephyrData, RestClient restClient) {
		long userId = ServerInfo.getUserId(restClient, getZephyrRestVersion(restClient));
		zephyrData.setUserId(userId);
	}

    private String getZephyrRestVersion(RestClient restClient) {
//        String zephyrVersion = ServerInfo.findZephyrVersion(restClient);
        String zephyrRestVersion = "v1";
//			if (zephyrVersion.equals("4.8") || zephyrVersion.equals("5.0")) {
//				zephyrRestVersion = "v1";
//			} else {
//				zephyrRestVersion = "latest";
//			}

        return zephyrRestVersion;
    }

	private void determineCyclePrefix(ZephyrConfigModel zephyrData) {
		if (StringUtils.isNotBlank(cyclePrefix)) {
			zephyrData.setCyclePrefix(cyclePrefix + "_");
		} else {
			zephyrData.setCyclePrefix(CYCLE_PREFIX_DEFAULT);
		}
	}

	private void determineProjectID(ZephyrConfigModel zephyrData, RestClient restClient) {
		long projectId = 0;
		try {
			projectId = Long.parseLong(projectKey);
		} catch (NumberFormatException e1) {
			logger.println("Project Key appears to be Name of the project");
			try {
				Long projectIdByName = Project.getProjectIdByName(projectKey, restClient, getZephyrRestVersion(restClient));
				projectId = projectIdByName;
			} catch (Exception e) {
				e.printStackTrace();
			}
			e1.printStackTrace();
		}
	
		zephyrData.setZephyrProjectId(projectId);
	}

	private void determineReleaseID(ZephyrConfigModel zephyrData, RestClient restClient) {

		long releaseId = 0;
		try {
			releaseId = Long.parseLong(releaseKey);
		} catch (NumberFormatException e1) {
			logger.println("Release Key appears to be Name of the Release");
			try {
				long releaseIdByReleaseNameProjectId = Release
						.getReleaseIdByNameProjectId(releaseKey, zephyrData.getZephyrProjectId(), restClient, getZephyrRestVersion(restClient));
				releaseId = releaseIdByReleaseNameProjectId;
			} catch (Exception e) {
				e.printStackTrace();
			}
			e1.printStackTrace();
		}
		zephyrData.setReleaseId(releaseId);
	}

	private void determineCycleID(ZephyrConfigModel zephyrData, RestClient restClient) {

		if (cycleKey.equalsIgnoreCase(NEW_CYCLE_KEY)) {
			zephyrData.setCycleId(NEW_CYCLE_KEY_IDENTIFIER);
			return;
		}
		long cycleId = 0;
		try {
			cycleId = Long.parseLong(cycleKey);
		} catch (NumberFormatException e1) {
			logger.println("Cycle Key appears to be the name of the cycle");
			try {
				Long cycleIdByCycleNameAndReleaseId = Cycle
						.getCycleIdByCycleNameAndReleaseId(cycleKey, zephyrData.getReleaseId(), restClient, getZephyrRestVersion(restClient));
				cycleId = cycleIdByCycleNameAndReleaseId;
			} catch (Exception e) {
				e.printStackTrace();
			}
			e1.printStackTrace();
		}
	
		zephyrData.setCycleId(cycleId);
	}


	@Override
	public ZeeDescriptor getDescriptor() {
		return (ZeeDescriptor) super.getDescriptor();
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

	public String getCycleDuration() {
		return cycleDuration;
	}

	public void setCycleDuration(String cycleDuration) {
		this.cycleDuration = cycleDuration;
	}

	public boolean isCreatePackage() {
		return createPackage;
	}

	public void setCreatePackage(boolean createPackage) {
		this.createPackage = createPackage;
	}

}
