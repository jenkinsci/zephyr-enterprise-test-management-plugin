package com.thed.zephyr.jenkins.reporter;

/**
 * @author mohan
 */

import static com.thed.zephyr.jenkins.reporter.ZeeConstants.ADD_ZEPHYR_GLOBAL_CONFIG;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_PREFIX_DEFAULT;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY_IDENTIFIER;

import com.google.gson.Gson;
import com.thed.model.*;
import com.thed.service.*;
import com.thed.service.impl.*;
import com.thed.utils.EggplantParser;
import com.thed.utils.ParserUtil;
import com.thed.utils.ZephyrConstants;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import hudson.tasks.junit.*;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import com.thed.zephyr.jenkins.model.ZephyrConfigModel;
import com.thed.zephyr.jenkins.model.ZephyrInstance;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ZeeReporter extends Notifier implements SimpleBuildStep {

	private String projectKey;
	private String releaseKey;
	private String cycleKey;
	private String cyclePrefix;
	private String serverAddress;
	private String cycleDuration;
	private boolean createPackage;
    private String resultXmlFilePath;
    private String parserIndex;
    private Integer eggplantParserIndex = 3;

    private String[] parserTemplateArr = new String[] {
            "{ \"packageName\": \"testsuite.testcase:classname\" , \"testcase\" : {\"name\": \"testsuite.testcase:name\", \"time\" : \"testsuite.testcase:time\", \"all\": {\"time\": \"testsuite:time\"}}}"
    };


	public static PrintStream logger;
	private static final String PluginName = "[Zephyr Enterprise Test Management";
    private static final String JUNIT_PFX = "TEST-*";
    private static final String SUREFIRE_REPORT = "surefire-reports";
    private static final String JUNIT_SFX = "/*.xml";
	private final String pInfo = String.format("%s [INFO]", PluginName);
    private String jenkinsProjectName;


    private UserService userService = new UserServiceImpl();
    private ProjectService projectService = new ProjectServiceImpl();
    private TCRCatalogTreeService tcrCatalogTreeService = new TCRCatalogTreeServiceImpl();
    private RequirementService requirementService = new RequirementServiceImpl();
    private TestcaseService testcaseService = new TestcaseServiceImpl();
    private CycleService cycleService = new CycleServiceImpl();
    private ExecutionService executionService = new ExecutionServiceImpl();
    private AttachmentService attachmentService = new AttachmentServiceImpl();

	@DataBoundConstructor
	public ZeeReporter(String serverAddress, String projectKey,
			String releaseKey, String cycleKey, String cyclePrefix,
			String cycleDuration, boolean createPackage, String resultXmlFilePath, String parserIndex) {
		this.serverAddress = serverAddress;
		this.projectKey = projectKey;
		this.releaseKey = releaseKey;
		this.cycleKey = cycleKey;
		this.cyclePrefix = cyclePrefix;
		this.createPackage = createPackage;
		this.cycleDuration = cycleDuration;
        this.resultXmlFilePath = resultXmlFilePath;
        this.parserIndex = parserIndex;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        perform(run, listener);
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher,
                           final BuildListener listener) throws IOException, InterruptedException {
        return perform(build, listener);
    }

	public boolean perform(final Run build, final TaskListener listener) throws IOException, InterruptedException {
		logger = listener.getLogger();
		logger.printf("%s Examining test results...%n", pInfo);

        requirementService = new RequirementServiceImpl();
        attachmentService = new AttachmentServiceImpl();

        parserTemplateArr = new String[] {
                "[{ \"status\": \"$testsuite.testcase.failure\", \"statusExistPass\": false, \"statusString\": null, \"statusFailAttachment\": \"$testsuite.testcase.failure\", \"statusPassAttachment\": \"classname: $testsuite.testcase:classname \nname: $testsuite.testcase:name \ntime: $testsuite.testcase:time\", \"packageName\": \"$testsuite.testcase:classname\" , \"skipTestcaseNames\": \"\", \"testcase\" : {\"name\": \"$testsuite.testcase:name\"}, \"requirements\": [{\"id\": \"$testsuite.testcase.requirements.requirement\"}], \"attachments\": [{\"filePath\": \"$testsuite.testcase.attachments.attachment\"}]}]",//junit
                "[{ \"status\": \"\", \"system-out\": \"$testsuite.testcase.system-out\", \"statusExistPass\": false, \"statusString\": null, \"statusFailAttachment\": \"\", \"statusPassAttachment\": \"classname: $testsuite.testcase:classname \nname: $testsuite.testcase:name \ntime: $testsuite.testcase:time\", \"packageName\": \"$testsuite.testcase:classname\" , \"skipTestcaseNames\": \"\", \"testcase\" : {\"name\": \"$testsuite.testcase:name\"}, \"requirements\": [{\"id\": \"$testsuite.testcase.requirements.requirement\"}], \"attachments\": [{\"filePath\": \"$testsuite.testcase.attachments.attachment\"}]}]", //cucumber
                "[{ \"status\": \"$testng-results.suite.test.class.test-method:status\", \"statusExistPass\": true, \"statusString\": \"PASS\", \"packageName\": \"$testng-results.suite.test.class:name\", \"skipTestcaseNames\": \"afterMethod,beforeMethod,afterClass,beforeClass,afterSuite,beforeSuite\", \"testcase\" : {\"name\": \"$testng-results.suite.test.class.test-method:name\"}}]", //testng
                "[{ \"status\": \"$testsuite.testcase:successes\", \"statusExistPass\": true, \"statusString\": \"1\", \"packageName\": \"$testsuite:name\" , \"skipTestcaseNames\": \"\", \"testcase\" : {\"name\": \"$testsuite.testcase:name\"}, \"requirements\": [{\"id\": \"$testsuite.testcase.requirements.requirement\"}]}]"//eggplant
        };

		if (!validateBuildConfig()) {
			logger.println("Cannot Proceed. Please verify the job configuration");
			return false;
		}

        jenkinsProjectName = ((FreeStyleBuild) build).getProject().getName();
		int number = build.getNumber();

        try {
            ZephyrConfigModel zephyrConfigModel = new ZephyrConfigModel();
            zephyrConfigModel.setZephyrProjectId(Long.parseLong(getProjectKey()));
            zephyrConfigModel.setReleaseId(Long.parseLong(getReleaseKey()));

            if (cycleKey.equalsIgnoreCase(NEW_CYCLE_KEY)) {
                zephyrConfigModel.setCycleId(NEW_CYCLE_KEY_IDENTIFIER);
            }
            else {
                zephyrConfigModel.setCycleId(Long.parseLong(getCycleKey()));
            }

            zephyrConfigModel.setCycleDuration(getCycleDuration());

            if (StringUtils.isNotBlank(getCyclePrefix())) {
                zephyrConfigModel.setCyclePrefix(getCyclePrefix() + "_");
            } else {
                zephyrConfigModel.setCyclePrefix(CYCLE_PREFIX_DEFAULT);
            }

            zephyrConfigModel.setCreatePackage(isCreatePackage());
            zephyrConfigModel.setResultXmlFilePath(getResultXmlFilePath());
            zephyrConfigModel.setParserIndex(Long.parseLong(getParserIndex()));

            zephyrConfigModel.setBuilNumber(number);

            ZephyrInstance zephyrInstance = getZephyrInstance(getServerAddress());
            zephyrConfigModel.setSelectedZephyrServer(zephyrInstance);

            //login to zephyr server

            boolean loggedIn = userService.login(zephyrInstance.getServerAddress(), zephyrInstance.getUsername(), zephyrInstance.getPassword());
            if(!loggedIn) {
                logger.println("Authorization for zephyr server failed.");
                return false;
            }

            //creating Map<testcaseName, passed>, Set<packageName> and set to zephyrConfigModel
            boolean prepareZephyrTests = prepareZephyrTests(build, zephyrConfigModel);
            //not checking for junit publisher
//            if(!prepareZephyrTests) {
//                logger.println("Error parsing surefire reports.");
//                logger.println("Please ensure \"Publish JUnit test result report is added\" as a post build action");
//                return false;
//            }

            List<Map> dataMapList = new ArrayList<>();

            List<String> xmlFiles = new ArrayList<>();

            if(Objects.equals(parserIndex, eggplantParserIndex)) {
                //use eggplant parser to find all related xml files
                EggplantParser eggplantParser = new EggplantParser("unknownSUT", "url");
                List<EggPlantResult> eggPlantResults = eggplantParser.invoke(new File(resolveRelativeFilePath(resultXmlFilePath)));
                xmlFiles.addAll(getTestcasesForEggplant(eggPlantResults));
            } else {
                xmlFiles.add(resolveRelativeFilePath(resultXmlFilePath));
            }

            for(String xmlFilePath : xmlFiles) {
                dataMapList.addAll(genericParserXML(xmlFilePath, parserTemplateArr[Integer.valueOf(String.valueOf(zephyrConfigModel.getParserIndex()))]));
            }

            zephyrConfigModel.setPackageNames(getPackageNamesFromXML(dataMapList));
            Map<String, TCRCatalogTreeDTO> packagePhaseMap = createPackagePhaseMap(zephyrConfigModel);
            Map<TCRCatalogTreeTestcase, Map<String, Object>> tcrStatusMap = createTestcasesFromMap(packagePhaseMap, dataMapList, zephyrConfigModel);

//            Map<CaseResult, TCRCatalogTreeTestcase> caseMap = createTestcases(zephyrConfigModel, packagePhaseMap);
//
            com.thed.model.Project project = projectService.getProjectById(zephyrConfigModel.getZephyrProjectId());

            com.thed.model.Cycle cycle;

            if(zephyrConfigModel.getCycleId() == NEW_CYCLE_KEY_IDENTIFIER) {

                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("E dd, yyyy hh:mm a");
                String dateFormatForCycleCreation = sdf.format(date);

                String cycleName = zephyrConfigModel.getCyclePrefix() + dateFormatForCycleCreation;

                cycle = new com.thed.model.Cycle();
                cycle.setName(cycleName);
                cycle.setReleaseId(zephyrConfigModel.getReleaseId());
                cycle.setBuild(String.valueOf(zephyrConfigModel.getBuilNumber()));
                cycle.setStartDate(project.getStartDate());

                Date projectStartDate = project.getStartDate();

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(projectStartDate);


                if(zephyrConfigModel.getCycleDuration().equals("30 days")) {
                    calendar.add(Calendar.DAY_OF_MONTH, 29);
                }
                else if(zephyrConfigModel.getCycleDuration().equals("7 days")) {
                    calendar.add(Calendar.DAY_OF_MONTH, 6);
                }
                cycle.setEndDate(calendar.getTime());

                cycle = cycleService.createCycle(cycle);
                zephyrConfigModel.setCycleId(cycle.getId());
            }
            else {
                cycle = cycleService.getCycleById(zephyrConfigModel.getCycleId());
            }

            CyclePhase cyclePhase = new CyclePhase();
            cyclePhase.setName(packagePhaseMap.get("parentPhase").getName());
            cyclePhase.setCycleId(cycle.getId());
            cyclePhase.setStartDate(new Date(cycle.getStartDate()));
            cyclePhase.setEndDate(new Date(cycle.getEndDate()));
            cyclePhase.setReleaseId(zephyrConfigModel.getReleaseId());
            cyclePhase.setFreeForm(true);

            cyclePhase = cycleService.createCyclePhase(cyclePhase);

            //adding testcases to free form cycle phase
            cycleService.addTestcasesToFreeFormCyclePhase(cyclePhase, new ArrayList<>(tcrStatusMap.keySet()), zephyrConfigModel.isCreatePackage());

            //assigning testcases in cycle phase to creator
            cycleService.assignCyclePhaseToCreator(cyclePhase.getId());

            List<ReleaseTestSchedule> releaseTestSchedules = executionService.getReleaseTestSchedules(cyclePhase.getId());

            Map<Boolean, Set<Long>> executionMap = new HashMap<>();
            executionMap.put(Boolean.TRUE, new HashSet<Long>());
            executionMap.put(Boolean.FALSE, new HashSet<Long>());

            List<TestStepResult> testStepResultList = new ArrayList<>();

            loop1 : for(Map.Entry<TCRCatalogTreeTestcase, Map<String, Object>> caseEntry : tcrStatusMap.entrySet()) {

                for(ReleaseTestSchedule releaseTestSchedule : releaseTestSchedules) {

                    if(Objects.equals(releaseTestSchedule.getTcrTreeTestcase().getTestcase().getId(), caseEntry.getKey().getTestcase().getId())) {
                        // tcrTestcase matched, map caseResult.isPass status to rtsId
                        executionMap.get(caseEntry.getValue().get("status")).add(releaseTestSchedule.getId());

                        TCRCatalogTreeTestcase tcrTestCase = caseEntry.getKey();

                        if(tcrTestCase.getTestcase().getTestSteps() != null && tcrTestCase.getTestcase().getTestSteps().getSteps() != null) {
                            List<Map<String, String>> stepList = (List<Map<String, String>>)caseEntry.getValue().get("stepList");

                            for (TestStepDetail testStepDetail : tcrTestCase.getTestcase().getTestSteps().getSteps()) {
                                for (Map<String, String> stepMap : stepList) {

                                    if(testStepDetail.getStep().equals(stepMap.get("step"))) {
                                        TestStepResult testStepResult = new TestStepResult();
                                        testStepResult.setCyclePhaseId(releaseTestSchedule.getCyclePhaseId());
                                        testStepResult.setReleaseTestScheduleId(releaseTestSchedule.getId());
                                        testStepResult.setTestStepId(testStepDetail.getOrderId());

                                        String status = "";

                                        if(stepMap.get("status").equalsIgnoreCase("true")) {
                                            status = ZephyrConstants.EXECUTION_STATUS_PASS;
                                        } else if(stepMap.get("status").equalsIgnoreCase("false")) {
                                            status = ZephyrConstants.EXECUTION_STATUS_FAIL;
                                        } else {
                                            status = ZephyrConstants.EXECUTION_STATUS_NOT_EXECUTED;
                                        }
                                        testStepResult.setStatus(Long.parseLong(status));
                                        testStepResultList.add(testStepResult);
                                    }
                                }
                            }
                        }

                        continue loop1;
                    }
                }
            }

            executionService.addTestStepResults(testStepResultList);
            executionService.executeReleaseTestSchedules(executionMap.get(Boolean.TRUE), Boolean.TRUE);
            executionService.executeReleaseTestSchedules(executionMap.get(Boolean.FALSE), Boolean.FALSE);
        }
        catch(Exception e) {
            //todo:handle exceptions gracefully
            e.printStackTrace();
            logger.printf("Error uploading test results to Zephyr");
            return false;
        }

		logger.printf("%s Done uploading tests to Zephyr.%n", pInfo);
		return true;
	}

    private Map<String, TCRCatalogTreeDTO> createPackagePhaseMap(ZephyrConfigModel zephyrConfigModel) throws URISyntaxException {

        List<TCRCatalogTreeDTO> tcrCatalogTreeDTOList = tcrCatalogTreeService.getTCRCatalogTreeNodes(ZephyrConstants.TCR_CATALOG_TREE_TYPE_PHASE, zephyrConfigModel.getReleaseId());

        String phaseDescription = zephyrConfigModel.isCreatePackage() ? ZephyrConstants.PACKAGE_TRUE_DESCRIPTION : ZephyrConstants.PACKAGE_FALSE_DESCRIPTION;
        boolean createPhase = true;
        TCRCatalogTreeDTO automationPhase = null;
        for(TCRCatalogTreeDTO tcrCatalogTreeDTO : tcrCatalogTreeDTOList) {
            if(tcrCatalogTreeDTO.getName().equals(ZephyrConstants.TOP_PARENT_PHASE_NAME)) {
                if((zephyrConfigModel.isCreatePackage() && tcrCatalogTreeDTO.getDescription().equals(ZephyrConstants.PACKAGE_TRUE_DESCRIPTION))
                        || (!zephyrConfigModel.isCreatePackage() && tcrCatalogTreeDTO.getDescription().equals(ZephyrConstants.PACKAGE_FALSE_DESCRIPTION))) {
                    automationPhase = tcrCatalogTreeDTO;
                    createPhase = false;
                }
            }
        }

        if(createPhase) {
            //top parent phase is not available which means we have to create this and all following phases
            automationPhase = tcrCatalogTreeService.createPhase(ZephyrConstants.TOP_PARENT_PHASE_NAME,
                    phaseDescription,
                    zephyrConfigModel.getReleaseId(),
                    0l);
        }

        Map<String, TCRCatalogTreeDTO> packagePhaseMap = new HashMap<>();
        packagePhaseMap.put("parentPhase", automationPhase);

        if(!zephyrConfigModel.isCreatePackage()) {
            return packagePhaseMap;
        }

        Set<String> packageNames = zephyrConfigModel.getPackageNames();

        for(String packageName : packageNames) {

            String[] packageNameArr = packageName.split("\\.");
            TCRCatalogTreeDTO endingNode = automationPhase; //This node is where testcases will be created, it is the last node to be created in this package hierarchy

            for (int i = 0; i < packageNameArr.length; i++) {
                String pName = packageNameArr[i];

                if(createPhase) {
                    //last phase searched doesn't exist and was created new, any following phases need to be created
                    endingNode = tcrCatalogTreeService.createPhase(pName, phaseDescription, zephyrConfigModel.getReleaseId(), endingNode.getId());
                }
                else {
                    //endingNode exists for this package level and we can search for following nodes

                    TCRCatalogTreeDTO searchedPhase = null;

                    if(endingNode.getCategories() != null && endingNode.getCategories().size() > 0) {
                        for (TCRCatalogTreeDTO tct : endingNode.getCategories()) {
                            if(tct.getName().equals(pName)) {
                                searchedPhase = tct;
                            }
                        }
                    }

                    if(searchedPhase == null) {
                        //no more children to this phase to search, need to create new phases
                        searchedPhase = tcrCatalogTreeService.createPhase(pName, phaseDescription, zephyrConfigModel.getReleaseId(), endingNode.getId());
                        createPhase = true;
                    }
                    endingNode = searchedPhase;
                }
            }
            packagePhaseMap.put(packageName, endingNode);
        }
        return packagePhaseMap;
    }

    private Map<CaseResult, TCRCatalogTreeTestcase> createTestcases(ZephyrConfigModel zephyrConfigModel, Map<String, TCRCatalogTreeDTO> packagePhaseMap) throws URISyntaxException {

        Map<CaseResult, TCRCatalogTreeTestcase> existingTestcases = new HashMap<>();
        Map<Long, List<CaseResult>> testcasesToBeCreated = new HashMap<>(); // treeId -> caseResult

        if(zephyrConfigModel.isCreatePackage()) {
            //need to create hierarchy

            Map<String, List<CaseResult>> packageCaseResultMap = zephyrConfigModel.getPackageCaseResultMap();
            Set<String> packageNameSet = packageCaseResultMap.keySet();

            for(String packageName : packageNameSet) {
                TCRCatalogTreeDTO tcrCatalogTreeDTO = packagePhaseMap.get(packageName);
                List<TCRCatalogTreeTestcase> tcrTestcases = testcaseService.getTestcasesForTreeId(tcrCatalogTreeDTO.getId());

                List<CaseResult> caseResults = packageCaseResultMap.get(packageName);

                caseResultLoop : for (CaseResult caseResult : caseResults) {
                    for (TCRCatalogTreeTestcase tcrTestcase : tcrTestcases) {
                        if(caseResult.getFullName().equals(tcrTestcase.getTestcase().getName())) {
                            existingTestcases.put(caseResult, tcrTestcase);
                            continue caseResultLoop;
                        }
                    }
                    //no match found for caseResult, need to create this
                    if(testcasesToBeCreated.containsKey(tcrCatalogTreeDTO.getId())) {
                        testcasesToBeCreated.get(tcrCatalogTreeDTO.getId()).add(caseResult);
                    }
                    else {
                        List<CaseResult> cr1 = new ArrayList<>();
                        cr1.add(caseResult);
                        testcasesToBeCreated.put(tcrCatalogTreeDTO.getId(), cr1);
                    }
                }
            }
        }
        else {
            //put everything in automation

            TCRCatalogTreeDTO tcrCatalogTreeDTO = packagePhaseMap.get("parentPhase");
            List<TCRCatalogTreeTestcase> tcrTestcases = testcaseService.getTestcasesForTreeId(tcrCatalogTreeDTO.getId());

            Map<String, List<CaseResult>> packageCaseResultMap = zephyrConfigModel.getPackageCaseResultMap();
            List<CaseResult> caseResults = new ArrayList<>();

            Collection<List<CaseResult>> tmpCol = packageCaseResultMap.values();
            for (List<CaseResult> tmpList : tmpCol) {
                caseResults.addAll(tmpList);
            }



            caseResultLoop : for (CaseResult caseResult : caseResults) {
                for (TCRCatalogTreeTestcase tcrTestcase : tcrTestcases) {
                    if(caseResult.getFullName().equals(tcrTestcase.getTestcase().getName())) {
                        existingTestcases.put(caseResult, tcrTestcase);
                        continue caseResultLoop;
                    }
                }
                //no match found for caseResult, need to create this
                if(testcasesToBeCreated.containsKey(tcrCatalogTreeDTO.getId())) {
                    testcasesToBeCreated.get(tcrCatalogTreeDTO.getId()).add(caseResult);
                }
                else {
                    List<CaseResult> cr1 = new ArrayList<>();
                    cr1.add(caseResult);
                    testcasesToBeCreated.put(tcrCatalogTreeDTO.getId(), cr1);
                }
            }
        }

        Map<CaseResult, TCRCatalogTreeTestcase> map = testcaseService.createTestcases(testcasesToBeCreated);
        existingTestcases.putAll(map);

        return existingTestcases;
    }

    private ZephyrInstance getZephyrInstance(String serverAddress) {
        List<ZephyrInstance> zephyrServers = getDescriptor().getZephyrInstances();

        for (ZephyrInstance zephyrInstance : zephyrServers) {
            if (StringUtils.isNotBlank(zephyrInstance.getServerAddress()) && zephyrInstance.getServerAddress().trim().equals(serverAddress)) {
                return zephyrInstance;
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
    private boolean prepareZephyrTests(final Run build, ZephyrConfigModel zephyrConfig) throws IOException, InterruptedException, ParserConfigurationException, SAXException {

        boolean status = true;
        Collection<SuiteResult> suites = new ArrayList<>();


        boolean isMavenProject = build.getClass().getName().toLowerCase().contains("maven");
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

        Map<String, List<CaseResult>> packageCaseMap = new HashMap<>();
		Integer noOfCases = prepareTestResults(suites, packageNames, packageCaseMap);

		logger.print("Total Test Cases : " + noOfCases);

        zephyrConfig.setPackageCaseResultMap(packageCaseMap);
//		zephyrConfig.setPackageNames(packageNames);
		
		return status;
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
	private Integer prepareTestResults(Collection<SuiteResult> suites, Set<String> packageNames, Map<String, List<CaseResult>> packageCaseMap) {
		Integer noOfCases = 0;
        for (Iterator<SuiteResult> iterator = suites.iterator(); iterator
				.hasNext();) {
			SuiteResult suiteResult = iterator.next();
			List<CaseResult> cases = suiteResult.getCases();
			for (CaseResult caseResult : cases) {
                String packageName = caseResult.getPackageName();
                if(packageCaseMap.containsKey(packageName)) {
                    packageCaseMap.get(packageName).add(caseResult);
                }
                else {
                    List<CaseResult> caseResults = new ArrayList<>();
                    caseResults.add(caseResult);
                    packageCaseMap.put(packageName, caseResults);
                }
                packageNames.add(packageName);
                noOfCases++;
			}
		}
		return noOfCases;
	}

    public Set<String> getPackageNamesFromXML(List<Map> dataMapList) throws ParserConfigurationException, SAXException, IOException {
        Set<String> packageNames = new HashSet<>();
        for (Map dataMap : dataMapList) {
            packageNames.add(dataMap.get("packageName").toString());
        }
        return packageNames;
    }

    public List<Map> genericParserXML(String absoluteFilePath, String parserTemplate) throws ParserConfigurationException, SAXException, IOException {
        ParserUtil parserUtil = new ParserUtil();
        return parserUtil.parseXmlLang(absoluteFilePath, parserTemplate);
    }

    public Map<TCRCatalogTreeTestcase, Map<String, Object>> createTestcasesFromMap(Map<String, TCRCatalogTreeDTO> packagePhaseMap, List<Map> dataMapList, ZephyrConfigModel zephyrConfigModel) throws URISyntaxException, IOException {
        Map<Long, List<Testcase>> treeIdTestcaseMap = new HashMap<>();
        Map<String, Map<String, Object>> testcaseNameStatusMap = new HashMap<>();

        dataMapLoop: for (Map dataMap : dataMapList) {

            Map testcaseMap = (Map) dataMap.get("testcase");

            String testcaseJson = new Gson().toJson(testcaseMap);
            Testcase testcase = new Gson().fromJson(testcaseJson, Testcase.class);

            if(testcase.getName().isEmpty()) {
                continue;
            }

            if(dataMap.containsKey("skipTestcaseNames")) {
                String[] skipTestcaseNames = dataMap.get("skipTestcaseNames").toString().split(",");
                for (String testcaseName : skipTestcaseNames) {
                    if(testcase.getName().equals(testcaseName)) {
                        //name matches, skip this testcase
                        continue dataMapLoop;
                    }
                }
            }

            String packageName = "parentPhase";
            if(isCreatePackage()) {
                packageName = dataMap.get("packageName").toString();
            }

            boolean status;

            if(dataMap.containsKey("statusString") && dataMap.get("statusString") != null) {
                boolean matched = dataMap.get("status").equals(dataMap.get("statusString"));
                if(Boolean.valueOf(dataMap.get("statusExistPass").toString())) {
                    status = matched;
                } else {
                    status = !matched;
                }
            } else {
                if(Boolean.valueOf(dataMap.get("statusExistPass").toString())) {
                    status = dataMap.get("status").toString().length() > 0;
                } else {
                    status = dataMap.get("status").toString().length() == 0;
                }
            }

            TCRCatalogTreeDTO treeDTO = packagePhaseMap.get(packageName);

            if(treeIdTestcaseMap.containsKey(treeDTO.getId())) {
                treeIdTestcaseMap.get(treeDTO.getId()).add(testcase);
            } else {
                List<Testcase> testcaseList = new ArrayList<>();
                testcaseList.add(testcase);
                treeIdTestcaseMap.put(treeDTO.getId(), testcaseList);
            }

            Set<Long> requirementIds = new HashSet<>();
            Set<String> requirementAltIds = new HashSet<>();
            if(dataMap.containsKey("requirements")) {
                List<Map> requirements = (List) dataMap.get("requirements");
                for (Map requirement : requirements) {
                    String id = requirement.get("id").toString();
                    String[] splitRequirementId = id.split("_");
                    if(id.startsWith("ID_")) {
                        requirementIds.add(Long.parseLong(splitRequirementId[1]));
                    } else if(id.startsWith("AltID_")) {
                        requirementAltIds.add(splitRequirementId[1]);
                    }
                }
            }

            MapTestcaseToRequirement mapTestcaseToRequirement = new MapTestcaseToRequirement();
            mapTestcaseToRequirement.setRequirementId(requirementIds);
            mapTestcaseToRequirement.setRequirementAltId(requirementAltIds);
            mapTestcaseToRequirement.setReleaseId(zephyrConfigModel.getReleaseId());

            List<String> attachments = new ArrayList<>();

            if(dataMap.containsKey("attachments")) {
                List<Map> attachmentFilePaths = (List) dataMap.get("attachments");
                for(Map attachment : attachmentFilePaths) {
                    String filePath = attachment.get("filePath").toString();
                    attachments.add(filePath);
                }
            }

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("mapTestcaseToRequirement", mapTestcaseToRequirement);
            valueMap.put("status", status);
            valueMap.put("attachments", attachments);

            if(status && dataMap.containsKey("statusPassAttachment")) {
                String successAttachmentStr = dataMap.get("statusPassAttachment").toString();
                if(!StringUtils.isEmpty(successAttachmentStr)) {
                    GenericAttachmentDTO genericAttachmentDTO = new GenericAttachmentDTO();
                    genericAttachmentDTO.setFileName("success.txt");
                    genericAttachmentDTO.setContentType("text/plain");
                    genericAttachmentDTO.setFieldName(AttachmentService.ItemType.TESTCASE.toString().toLowerCase());
                    genericAttachmentDTO.setByteData(successAttachmentStr.getBytes());
                    valueMap.put("statusAttachment", genericAttachmentDTO);
                }
            }

            if(!status && dataMap.containsKey("statusFailAttachment")) {
                String failureAttachmentStr = dataMap.get("statusFailAttachment").toString();
                if(!StringUtils.isEmpty(failureAttachmentStr)) {
                    GenericAttachmentDTO genericAttachmentDTO = new GenericAttachmentDTO();
                    genericAttachmentDTO.setFileName("failure.txt");
                    genericAttachmentDTO.setContentType("text/plain");
                    genericAttachmentDTO.setFieldName(AttachmentService.ItemType.TESTCASE.toString().toLowerCase());
                    genericAttachmentDTO.setByteData(failureAttachmentStr.getBytes());
                    valueMap.put("statusAttachment", genericAttachmentDTO);
                }
            }

            if(dataMap.containsKey("system-out")) {
                String stepStr = dataMap.get("system-out").toString();
                if(!StringUtils.isEmpty(stepStr)) {
                    List<Map<String, String>> stepList = new ArrayList<>();
                    TestStep testStep = stepMaker(stepStr, stepList);
                    valueMap.put("stepList", stepList);
                    testcase.setTestSteps(testStep);
                }
            }

            testcaseNameStatusMap.put(testcase.getName(), valueMap);
        }
        List<TCRCatalogTreeTestcase> tcrList =  testcaseService.createTestcasesWithList(treeIdTestcaseMap);
        Map<TCRCatalogTreeTestcase, Map<String, Object>> tcrTestcaseStatusMap = new HashMap<>();
        List<MapTestcaseToRequirement> mapTestcaseToRequirements = new ArrayList<>();
        Map<Long, List<String>> testcaseAttachmentsMap = new HashMap<>();
        Map<Long, GenericAttachmentDTO> statusAttachmentMap = new HashMap<>();
        loop1 : for (Map.Entry<String, Map<String, Object>> entry : testcaseNameStatusMap.entrySet()) {
            for(TCRCatalogTreeTestcase tcrCatalogTreeTestcase : tcrList) {
                if(tcrCatalogTreeTestcase.getTestcase().getName().equals(entry.getKey())) {
                    //same testcase, add id and status to map
                    Map<String, Object> statusMap = new HashMap<>();
                    statusMap.put("status", entry.getValue().get("status"));

                    MapTestcaseToRequirement mapTestcaseToRequirement = (MapTestcaseToRequirement)entry.getValue().get("mapTestcaseToRequirement");
                    mapTestcaseToRequirement.setTestcaseId(tcrCatalogTreeTestcase.getTestcase().getId());
                    mapTestcaseToRequirements.add(mapTestcaseToRequirement);
                    testcaseAttachmentsMap.put(tcrCatalogTreeTestcase.getTestcase().getTestcaseId(), (List<String>) entry.getValue().get("attachments"));
                    if(entry.getValue().containsKey("statusAttachment")) {
                        statusAttachmentMap.put(tcrCatalogTreeTestcase.getTestcase().getTestcaseId(), (GenericAttachmentDTO) entry.getValue().get("statusAttachment"));
                    }

                    if(entry.getValue().containsKey("stepList")) {
                        statusMap.put("stepList", entry.getValue().get("stepList"));
                    }
                    tcrTestcaseStatusMap.put(tcrCatalogTreeTestcase, statusMap);
                    continue loop1;
                }
            }
        }
        List<String> msgs = requirementService.mapTestcaseToRequirements(mapTestcaseToRequirements);
        attachmentService.addAttachments(AttachmentService.ItemType.TESTCASE, testcaseAttachmentsMap, statusAttachmentMap);
        logger.println(msgs);
        return tcrTestcaseStatusMap;
    }

    TestStep stepMaker(String testSteps, List<Map<String, String>> stepsList) {
        String[] keyWords={"And","Or","Given","When","Then"};
        List<String> keywordsList= Arrays.asList(keyWords);
        String steps[] = testSteps.split("\\r?\\n");
        String status="";
        TestStep testStep = new TestStep();
        Integer i=1;
        for(String step : steps) {
            if(keywordsList.contains(step.split(" ", 2)[0])) {
                TestStepDetail testStepDetail = new TestStepDetail();
                Map<String, String> stepMap = new HashMap<String, String>();
                testStepDetail.setOrderId((long)i);
                testStepDetail.setStep(step.substring(0,step.indexOf(".")));
                stepMap.put("orderId", i.toString());
                stepMap.put("step", step.substring(0,step.indexOf(".")));
                status=step.substring(step.lastIndexOf(".")+1, step.length());
                if(status.equals("failed")) {
                    stepMap.put("status", "false");
                }else if(status.equals("skipped") || status.equals("undefined")) {
                    stepMap.put("status", "skipped");
                }else {
                    stepMap.put("status", "true");
                }
                stepsList.add(stepMap);
                testStep.getSteps().add(testStepDetail);
            }
            i++;
        }
        testStep.setMaxId((long)testStep.getSteps().size());
//        System.out.println(stepsList);
        return testStep;
    }

    private List<String> getTestcasesForEggplant(List<EggPlantResult> eggPlantResults) throws ParseException, ParserConfigurationException, SAXException, IOException {
        String scriptNameParseTemplate = "[{\"scriptName\": \"$testsuite:name\"}]";
        ParserUtil parserUtil = new ParserUtil();
        Map<String, EggPlantResult> eggPlantMap = new HashMap<>();//suite name, eggPlantResult
        for (EggPlantResult eggPlantResult : eggPlantResults) {
            List<Map> parseData = parserUtil.parseXmlLang(eggPlantResult.getXmlResultFile(), scriptNameParseTemplate);
            EggPlantResult existingEPR = eggPlantMap.get(parseData.get(0).get("scriptName").toString());
            if (existingEPR == null || existingEPR.getRunDateInDate().before(eggPlantResult.getRunDateInDate())) {
                //either the file path for this eggplant script doesn't exist in map or it is older
                eggPlantMap.put(parseData.get(0).get("scriptName").toString(), eggPlantResult);
            }
        }
        List<String> xmlFilePaths = new ArrayList<>();
        eggPlantMap.forEach((key, value) -> xmlFilePaths.add(value.getXmlResultFile()));
        return xmlFilePaths;
    }

    private String resolveRelativeFilePath(String resultXmlFilePath) {
        return Jenkins.get().getWorkspaceFor(Jenkins.get().getItem(jenkinsProjectName)) + File.separator + resultXmlFilePath;
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

    public String getResultXmlFilePath() {
        return resultXmlFilePath;
    }

    public void setResultXmlFilePath(String resultXmlFilePath) {
        this.resultXmlFilePath = resultXmlFilePath;
    }

    public String getParserIndex() {
        return parserIndex;
    }

    public void setParserIndex(String parserIndex) {
        this.parserIndex = parserIndex;
    }
}
