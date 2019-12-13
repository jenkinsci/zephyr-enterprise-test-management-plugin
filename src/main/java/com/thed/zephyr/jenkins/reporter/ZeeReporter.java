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
import java.net.URISyntaxException;
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
	private String cycleKey;;
	private String cyclePrefix;
	private String serverAddress;
	private String cycleDuration;
	private boolean createPackage;
    private String resultXmlFilePath;
    private Integer parserIndex;

    String parseMap1 = "{\"name\": \"testsuite.testcase1:time\"}";

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
    private TestcaseService testcaseService = new TestcaseServiceImpl();
    private CycleService cycleService = new CycleServiceImpl();
    private ExecutionService executionService = new ExecutionServiceImpl();

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
        this.parserIndex = Integer.parseInt(parserIndex);
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

        parserTemplateArr = new String[] {
                "{ \"status\": \"$testsuite.testcase.failure\", \"statusString\": null, \"packageName\": \"$testsuite.testcase:classname\" , \"testcase\" : {\"name\": \"$testsuite.testcase:name\"}}",//junit
                "{ \"status\": \"$testsuite.testcase.failure\", \"statusString\": null, \"packageName\": \"$testsuite.testcase:classname\" , \"testcase\" : {\"name\": \"$testsuite.testcase:name\"}}", //cucumber
                "{ \"status\": \"$testng-results.suite.test.class.test-method:status\", \"statusString\": \"PASS\", \"packageName\": \"$testng-results.suite.test.class:name\" , \"testcase\" : {\"name\": \"$testng-results.suite.test:name\"}}" //testng
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

            zephyrConfigModel.setPackageNames(getPackageNamesFromXML());
            Map<String, TCRCatalogTreeDTO> packagePhaseMap = createPackagePhaseMap(zephyrConfigModel);
//            parseXML(packagePhaseMap);
            Map<TCRCatalogTreeTestcase, Boolean> tcrStatusMap = genericParserXML(packagePhaseMap, parserTemplateArr[parserIndex]);

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

            loop1 : for(Map.Entry<TCRCatalogTreeTestcase, Boolean> caseEntry : tcrStatusMap.entrySet()) {

                for(ReleaseTestSchedule releaseTestSchedule : releaseTestSchedules) {

                    if(Objects.equals(releaseTestSchedule.getTcrTreeTestcase().getTestcase().getId(), caseEntry.getKey().getTestcase().getId())) {
                        // tcrTestcase matched, map caseResult.isPass status to rtsId
                        executionMap.get(caseEntry.getValue()).add(releaseTestSchedule.getId());
                        continue loop1;
                    }
                }
            }

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
        packageNames = getPackageNamesFromXML();

		logger.print("Total Test Cases : " + noOfCases);

        zephyrConfig.setPackageCaseResultMap(packageCaseMap);
		zephyrConfig.setPackageNames(packageNames);
		
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

    public Set<String> getPackageNamesFromXML() throws ParserConfigurationException, SAXException, IOException {
        String xmlFilePath = Jenkins.get().getWorkspaceFor(Jenkins.get().getItem(jenkinsProjectName)) + File.separator + resultXmlFilePath;
        ParserUtil parserUtil = new ParserUtil();
        List<Map> dataMapList = parserUtil.parseXmlLang(xmlFilePath, parserTemplateArr[parserIndex]);

        Set<String> packageNames = new HashSet<>();
        for (Map dataMap : dataMapList) {
            packageNames.add(dataMap.get("packageName").toString());
        }
        return packageNames;
    }

    public Map<TCRCatalogTreeTestcase, Boolean> genericParserXML(Map<String, TCRCatalogTreeDTO> packagePhaseMap, String parserTemplate) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        String xmlFilePath = Jenkins.get().getWorkspaceFor(Jenkins.get().getItem(jenkinsProjectName)) + File.separator + resultXmlFilePath;
        ParserUtil parserUtil = new ParserUtil();
        List<Map> dataMapList = parserUtil.parseXmlLang(xmlFilePath, parserTemplate);

        Map<Long, List<Testcase>> treeIdTestcaseMap = new HashMap<>();
        Map<String, Boolean> testcaseNameStatusMap = new HashMap<>();

        for (Map dataMap : dataMapList) {

            Map testcaseMap = (Map) dataMap.get("testcase");

            String testcaseJson = new Gson().toJson(testcaseMap);
            Testcase testcase = new Gson().fromJson(testcaseJson, Testcase.class);

            String packageName = "parentPhase";
            if(isCreatePackage()) {
                packageName = dataMap.get("packageName").toString();
            }

            boolean status;

            if(dataMap.containsKey("statusString") && dataMap.get("statusString") != null) {
                status = dataMap.get("status").equals(dataMap.get("statusString"));
            } else {
                status = dataMap.get("status").toString().length() > 0;
            }

            TCRCatalogTreeDTO treeDTO = packagePhaseMap.get(packageName);

            if(treeIdTestcaseMap.containsKey(treeDTO.getId())) {
                treeIdTestcaseMap.get(treeDTO.getId()).add(testcase);
            } else {
                List<Testcase> testcaseList = new ArrayList<>();
                testcaseList.add(testcase);
                treeIdTestcaseMap.put(treeDTO.getId(), testcaseList);
            }
            testcaseNameStatusMap.put(testcase.getName(), status);
        }
        List<TCRCatalogTreeTestcase> tcrList =  testcaseService.createTestcasesWithList(treeIdTestcaseMap);
        Map<TCRCatalogTreeTestcase, Boolean> testcaseIdStatusMap = new HashMap<>();
        loop1 : for (Map.Entry<String, Boolean> entry : testcaseNameStatusMap.entrySet()) {
            for(TCRCatalogTreeTestcase tcrCatalogTreeTestcase : tcrList) {
                if(tcrCatalogTreeTestcase.getTestcase().getName().equals(entry.getKey())) {
                    //same testcase, add id and status to map
                    testcaseIdStatusMap.put(tcrCatalogTreeTestcase, entry.getValue());
                    continue loop1;
                }
            }
        }
        return testcaseIdStatusMap;
    }

    public void parseXML(Map<String, TCRCatalogTreeDTO> packagePhaseMap) throws ParserConfigurationException, IOException, SAXException, URISyntaxException {

        Map<Long, List<Testcase>> treeIdTestcaseMap = new HashMap<>();

        File xmlFile = new File(Jenkins.get().getWorkspaceFor(Jenkins.get().getItem(jenkinsProjectName)) + File.separator + resultXmlFilePath);
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
        document.getDocumentElement().normalize();

        Element rootElement =  document.getDocumentElement();

        NodeList testcaseNodeList = rootElement.getElementsByTagName("testcase");

        JSONObject parseMapJson = new JSONObject(parseMap1);

        for (int i = 0; i < testcaseNodeList.getLength(); i++) {
            org.w3c.dom.Node testcaseNode = testcaseNodeList.item(i);

            JSONObject testcaseJson = new JSONObject(parseMapJson.toString());

            if(testcaseNode.getNodeType() == Node.ELEMENT_NODE) {
                Element testcaseElement = (Element) testcaseNode;
                String nameSyntax = parseMapJson.getString("name");
                String keys[] = nameSyntax.split("\\.");
                String lastKey = keys[keys.length-1];

                if(lastKey.contains(":")) {
                    String lk[] = lastKey.split(":");
                    lastKey = lk[lk.length-1];

                    testcaseJson.put("name", testcaseElement.getAttribute(lastKey));
                } else {
                    testcaseJson.put("name", testcaseElement.getTextContent());
                }
                Testcase testcase = new Gson().fromJson(testcaseJson.toString(), Testcase.class);

                String packageName = testcaseElement.getAttribute("classname");
                packageName = packageName.substring(0, packageName.lastIndexOf("."));

                TCRCatalogTreeDTO treeDTO = packagePhaseMap.get(packageName);

                if(treeIdTestcaseMap.containsKey(treeDTO.getId())) {
                    treeIdTestcaseMap.get(treeDTO.getId()).add(testcase);
                } else {
                    List<Testcase> testcaseList = new ArrayList<>();
                    testcaseList.add(testcase);
                    treeIdTestcaseMap.put(treeDTO.getId(), testcaseList);
                }
            }
        }
        testcaseService.createTestcasesWithList(treeIdTestcaseMap);
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
}
