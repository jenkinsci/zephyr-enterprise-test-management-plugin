package com.thed.zephyr.jenkins.reporter;

/**
 * @author mohan
 */

import static com.thed.zephyr.jenkins.reporter.ZeeConstants.ADD_ZEPHYR_GLOBAL_CONFIG;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_PREFIX_DEFAULT;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY_IDENTIFIER;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.google.gson.Gson;
import com.thed.model.*;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.thed.model.CyclePhase;
import com.thed.model.ReleaseTestSchedule;
import com.thed.model.TCRCatalogTreeDTO;
import com.thed.model.TCRCatalogTreeTestcase;
import com.thed.service.*;
import com.thed.service.impl.*;
import com.thed.utils.EggplantParser;
import com.thed.utils.ParserUtil;
import com.thed.utils.ZephyrConstants;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.*;
import hudson.tasks.junit.*;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import com.thed.zephyr.jenkins.model.ZephyrConfigModel;
import com.thed.zephyr.jenkins.model.ZephyrInstance;
import org.xml.sax.SAXException;

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
    private Long eggplantParserIndex = 3l;
    private String parserTemplateKey;


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
    private ParserTemplateService parserTemplateService = new ParserTemplateServiceImpl();
    private TestStepService testStepService = new TestStepServiceImpl();
    private PreferenceService preferenceService = new PreferenceServiceImpl();

	@DataBoundConstructor
	public ZeeReporter(String serverAddress, String projectKey,
			String releaseKey, String cycleKey, String cyclePrefix,
			String cycleDuration, boolean createPackage, String resultXmlFilePath, String parserTemplateKey) {
		this.serverAddress = serverAddress;
		this.projectKey = projectKey;
		this.releaseKey = releaseKey;
		this.cycleKey = cycleKey;
		this.cyclePrefix = cyclePrefix;
		this.createPackage = createPackage;
		this.cycleDuration = cycleDuration;
        this.resultXmlFilePath = resultXmlFilePath;
        this.parserTemplateKey = parserTemplateKey;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        jenkinsProjectName = workspace.getName();
        perform(run, listener);
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher,
                           final BuildListener listener) throws IOException, InterruptedException {
        jenkinsProjectName = ((FreeStyleBuild) build).getProject().getName();
        return perform(build, listener);
    }

	public boolean perform(final Run build, final TaskListener listener) throws IOException, InterruptedException {
		logger = listener.getLogger();
		logger.printf("%s Examining test results...%n", pInfo);

		if (!validateBuildConfig()) {
			logger.println("Cannot Proceed. Please verify the job configuration");
			return false;
		}

		int number = build.getNumber();

        try {

            ZephyrInstance zephyrInstance = getZephyrInstance(getServerAddress());

            //login to zephyr server
            StandardCredentials upCredentials = getCredentialsFromId(zephyrInstance.getCredentialsId());
            Boolean loggedIn = Boolean.FALSE;
            if(upCredentials instanceof UsernamePasswordCredentialsImpl){
                String user = ((UsernamePasswordCredentialsImpl) upCredentials).getUsername();
                String pass = ((UsernamePasswordCredentialsImpl) upCredentials).getPassword().getPlainText();
                loggedIn = userService.login(zephyrInstance.getServerAddress(),user,pass);
            }else if(upCredentials instanceof StringCredentialsImpl){
                String secretText = ((StringCredentialsImpl) upCredentials).getSecret().getPlainText();
                loggedIn = userService.login(zephyrInstance.getServerAddress(), secretText);
            }
            if(!loggedIn) {
                logger.println("Authorization for zephyr server failed.");
                return false;
            }

            ZephyrConfigModel zephyrConfigModel = new ZephyrConfigModel();
            zephyrConfigModel.setZephyrProjectId(Long.parseLong(getProjectKey()));
            zephyrConfigModel.setReleaseId(Long.parseLong(getReleaseKey()));
            zephyrConfigModel.setParserTemplateId(Long.parseLong(getParserTemplateKey()));
            zephyrConfigModel.setJsonParserTemplate(parserTemplateService.getParserTemplateById(zephyrConfigModel.getParserTemplateId()).getJsonTemplate());

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

            zephyrConfigModel.setBuilNumber(number);
            zephyrConfigModel.setSelectedZephyrServer(zephyrInstance);

            //creating Map<testcaseName, passed>, Set<packageName> and set to zephyrConfigModel
            boolean prepareZephyrTests = prepareZephyrTests(build, zephyrConfigModel);

            List<Map> dataMapList = new ArrayList<>();

            Set<String> xmlFiles = new HashSet<>();

            List<String> resultFilePathList = getAllIncludedFilePathList(getWorkspacePath(), resultXmlFilePath);

            for(String resultFilePath : resultFilePathList) {
                if(Objects.equals(zephyrConfigModel.getParserTemplateId(), eggplantParserIndex)) {
                    //use eggplant parser to find all related xml files
                    EggplantParser eggplantParser = new EggplantParser("unknownSUT", "url");
                    List<EggPlantResult> eggPlantResults = eggplantParser.invoke(new File(resultFilePath));
                    xmlFiles.addAll(getTestcasesForEggplant(eggPlantResults));
                } else {
                    xmlFiles.add(resultFilePath);
                }
            }

            for(String xmlFilePath : xmlFiles) {

                List<Map> currentDataMapList = genericParserXML(xmlFilePath, zephyrConfigModel.getJsonParserTemplate());
                File file = new File(xmlFilePath);

                for(Map dataMap : currentDataMapList) {
                    dataMap.put("basePath", file.getParent());
                }
                dataMapList.addAll(currentDataMapList);
            }

            //zephyrConfigModel.setPackageNames(getPackageNamesFromXML(dataMapList));
            zephyrConfigModel.setPackageNames(getPackageNamesFromXML(dataMapList));
            Map<String, TCRCatalogTreeDTO> packagePhaseMap = createPackagePhaseMap(zephyrConfigModel);
            Map<TCRCatalogTreeTestcase, Map<String, Object>> tcrStatusMap = createTestcasesFromMap(packagePhaseMap, dataMapList, zephyrConfigModel);

            logger.println("Total Test Cases : " + tcrStatusMap.keySet().size());

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
            cyclePhase.setStartDate(cycle.getStartDate());
            cyclePhase.setEndDate(cycle.getEndDate());
            cyclePhase.setReleaseId(zephyrConfigModel.getReleaseId());
            cyclePhase.setFreeForm(true);
            cyclePhase = cycleService.createCyclePhase(cyclePhase);

            //adding testcases to free form cycle phase
            cycleService.addTestcasesToFreeFormCyclePhase(cyclePhase, new ArrayList<>(tcrStatusMap.keySet()), zephyrConfigModel.isCreatePackage());

            //assigning testcases in cycle phase to creator
            List<ReleaseTestSchedule> releaseTestSchedules = cycleService.assignCyclePhaseToUser(cyclePhase, userService.getCurrentUser().getId());

            Map<String, Set<Long>> executionMap = new HashMap<>();

            Map<Long, List<String>> testcaseAttachmentsMap = new HashMap<>();
            Map<Long, GenericAttachmentDTO> statusAttachmentMap = new HashMap<>();

            List<TestStepResult> testStepResultList = new ArrayList<>();

            loop1 : for(Map.Entry<TCRCatalogTreeTestcase, Map<String, Object>> caseEntry : tcrStatusMap.entrySet()) {

                for(ReleaseTestSchedule releaseTestSchedule : releaseTestSchedules) {

                    if(Objects.equals(releaseTestSchedule.getTcrTreeTestcase().getTestcase().getId(), caseEntry.getKey().getTestcase().getId())) {
                        // tcrTestcase matched, map caseResult.isPass status to rtsId
                        Map<String, Object> testcaseValueMap = caseEntry.getValue();

                        if(testcaseValueMap.containsKey("statusId")) {
                            String statusId = testcaseValueMap.get("statusId").toString();
                            if(!executionMap.containsKey(statusId)) {
                                executionMap.put(statusId, new HashSet<Long>());
                            }
                            executionMap.get(statusId).add(releaseTestSchedule.getId());
                        }

                        if(testcaseValueMap.containsKey("attachments")) {
                            List<String> attachmentPathList = (List<String>)testcaseValueMap.get("attachments");
                            if(!attachmentPathList.isEmpty()) {
                                testcaseAttachmentsMap.put(releaseTestSchedule.getId(), attachmentPathList);
                            }
                        }

                        if(testcaseValueMap.containsKey("statusAttachment")) {
                            statusAttachmentMap.put(releaseTestSchedule.getId(), (GenericAttachmentDTO) testcaseValueMap.get("statusAttachment"));
                        }

                        TCRCatalogTreeTestcase tcrTestCase = caseEntry.getKey();

                        //testStepResult handled here
                        if(testcaseValueMap.containsKey("stepList")) {
                            //we have parsed steps from xml, this testcase existed before and the teststeps weren't fetched, fetching now
                            tcrTestCase.getTestcase().setTestSteps(testStepService.getTestStep(tcrTestCase.getTestcase().getId()));
                        }

                        if(tcrTestCase.getTestcase().getTestSteps() != null && tcrTestCase.getTestcase().getTestSteps().getSteps() != null) {
                            List<Map<String, String>> stepList = (List<Map<String, String>>)testcaseValueMap.get("stepList");

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
                                        stepList.remove(stepMap);
                                        break;
                                    }
                                }
                            }
                        }

                        continue loop1;
                    }
                }
            }

            List<String> errorLogs = attachmentService.addAttachments(AttachmentService.ItemType.releaseTestSchedule, testcaseAttachmentsMap, statusAttachmentMap);
            errorLogs.forEach(errorLog -> logger.println(errorLog));

            if(!testStepResultList.isEmpty()) {
                executionService.addTestStepResults(testStepResultList);
            }

            Set<String> activeStatusIdSet = preferenceService.getTestcaseExecutionStatusIds(true);
            for(Map.Entry<String, Set<Long>> entry : executionMap.entrySet()) {
                String statusId = entry.getKey();
                if(activeStatusIdSet.contains(statusId)) {
                    executionService.executeReleaseTestSchedules(entry.getValue(), statusId);
                } else {
                    logger.println("No active testcase execution status found for id: " + statusId);
                }
            }
        }
        catch(Exception e) {
            //todo:handle exceptions gracefully
            e.printStackTrace();
            logger.println("Error uploading test results to Zephyr");
            logger.println(e.getMessage());
            for(StackTraceElement stackTraceElement : e.getStackTrace()) {
                logger.println(stackTraceElement.toString());
            }
            return false;
        } finally {
            userService.getZephyrRestService().closeHttpConnection();
        }

		logger.printf("%s Done uploading tests to Zephyr.%n", pInfo);
		return true;
	}

    private Map<String, TCRCatalogTreeDTO> createPackagePhaseMap(ZephyrConfigModel zephyrConfigModel) throws URISyntaxException, IOException {

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
            //todo: make hierarchy locally instead of these extra rest calls
            automationPhase = tcrCatalogTreeService.getTCRCatalogTreeNode(automationPhase.getId()); //To refresh child nodes already created in the loop below
            TCRCatalogTreeDTO endingNode = automationPhase; //This node is where testcases will be created, it is the last node to be created in this package hierarchy
            createPhase = false;
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

    private Map<CaseResult, TCRCatalogTreeTestcase> createTestcases(ZephyrConfigModel zephyrConfigModel, Map<String, TCRCatalogTreeDTO> packagePhaseMap) throws URISyntaxException, IOException {

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

    private List<TCRCatalogTreeTestcase> createTestcasesWithoutDuplicate(Map<Long, List<Testcase>> treeIdTestcaseMap) throws URISyntaxException, IOException {

        List<TCRCatalogTreeTestcase> existingTestcases = new ArrayList<>();
        Map<Long, List<Testcase>> toBeCreatedTestcases = new HashMap<>();

        for(Map.Entry<Long, List<Testcase>> entry : treeIdTestcaseMap.entrySet()) {
            Long treeId = entry.getKey();
            List<Testcase> testcaseList = entry.getValue();

            List<TCRCatalogTreeTestcase> tcrTestcases = testcaseService.getTestcasesForTreeId(treeId);

            if(tcrTestcases == null || tcrTestcases.isEmpty()) {
                //no testcases exist for this tree, add need to be created
                List<Testcase> addToList = toBeCreatedTestcases.get(treeId);
                if(addToList == null) {
                    addToList = new ArrayList<>();
                    addToList.addAll(testcaseList);
                    toBeCreatedTestcases.put(treeId, addToList);
                } else {
                    addToList.addAll(testcaseList);
                }
                continue;
            }

            testcaseLoop : for(Testcase testcase : testcaseList) {
                for (TCRCatalogTreeTestcase tcrCatalogTreeTestcase : tcrTestcases) {
                    if(tcrCatalogTreeTestcase.getTestcase().getName().equals(testcase.getName())) {
                        //this testcase already exists in this tree, no need to create
                        existingTestcases.add(tcrCatalogTreeTestcase);
                        tcrTestcases.remove(tcrCatalogTreeTestcase);
                        continue testcaseLoop;
                    }
                }

                //on previous testcase, need to create
                List<Testcase> addToList = toBeCreatedTestcases.get(treeId);
                if(addToList == null) {
                    addToList = new ArrayList<>();
                    addToList.add(testcase);
                    toBeCreatedTestcases.put(treeId, addToList);
                } else {
                    addToList.add(testcase);
                }
            }
        }

        if(!toBeCreatedTestcases.isEmpty()) {
            List<TCRCatalogTreeTestcase> tcrList = testcaseService.createTestcasesWithList(toBeCreatedTestcases);
            existingTestcases.addAll(tcrList);
        }

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

    private StandardCredentials getCredentialsFromId(String credentialsId) {
        Iterable<StandardCredentials> credentials = CredentialsProvider.lookupCredentials(StandardCredentials.class,
                Jenkins.getInstance(),
                ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList());

        return CredentialsMatchers.firstOrNull(
                credentials,
                CredentialsMatchers.withId(credentialsId));
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
        List<Map<String, Map<String, Object>>> testcaseNameValueMapList = new ArrayList<>();

        long statusAttachmentCount = 0;

        dataMapLoop: for (Map dataMap : dataMapList) {

            Map testcaseMap = (Map) dataMap.get("testcase");

            String testcaseJson = new Gson().toJson(testcaseMap);
            Testcase testcase = new Gson().fromJson(testcaseJson, Testcase.class);
            if(testcase.getAutomated() == null) {
                testcase.setAutomated(true);
                testcase.setScriptName("Created By Jenkins");
            }

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

            Map<String, String> statusCondition = new HashMap<>();
            if(dataMap.containsKey("statuses") && dataMap.get("statuses") != null) {
                List<Map> statuses = (List) dataMap.get("statuses");
                boolean matched = false;
                for(Map sc : statuses) {
                    if((sc.get("statusString") == null && sc.get("status") != null && StringUtils.isNotEmpty(sc.get("status").toString())) //status is not empty
                            || (sc.get("statusString") != null && sc.get("statusString").equals(sc.get("status")))) { //status matches statusString
                        statusCondition = sc;
                        matched = true;
                        break;
                    }
                }
                if(!matched) {
                    //none of the status condition satisfied, search for default
                    for(Map sc : statuses) {
                        if(sc.get("default") != null && Boolean.valueOf(sc.get("default").toString())) { //default status
                            statusCondition = sc;
                            break;
                        }
                    }
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

            if(dataMap.containsKey("basePath") && statusCondition.containsKey("attachmentFileIncludes")) {
                String basePath = dataMap.get("basePath").toString();
                String includesStr = statusCondition.get("attachmentFileIncludes");
                attachments.addAll(getAllIncludedFilePathList(basePath, includesStr));
            }

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("treeId", treeDTO.getId());
            valueMap.put("mapTestcaseToRequirement", mapTestcaseToRequirement);
            if(statusCondition.containsKey("statusId")) {
                valueMap.put("statusId", statusCondition.get("statusId"));
            }
            valueMap.put("attachments", attachments);

            if(statusCondition.containsKey("attachmentText")) {
                String successAttachmentStr = statusCondition.get("attachmentText");
                if(!StringUtils.isEmpty(successAttachmentStr)) {
                    GenericAttachmentDTO genericAttachmentDTO = new GenericAttachmentDTO();
                    genericAttachmentDTO.setFileName("status_" + ++statusAttachmentCount + "_" + System.currentTimeMillis() + ".txt");
                    genericAttachmentDTO.setContentType("text/plain");
                    genericAttachmentDTO.setFieldName(AttachmentService.ItemType.releaseTestSchedule.toString());
                    genericAttachmentDTO.setByteData(successAttachmentStr.getBytes());
                    valueMap.put("statusAttachment", genericAttachmentDTO);
                }
            }

            if(dataMap.containsKey("stepText")) {
                String stepStr = dataMap.get("stepText").toString();
                if(!StringUtils.isEmpty(stepStr)) {
                    List<Map<String, String>> stepList = new ArrayList<>();
                    TestStep testStep = stepMaker(stepStr, stepList);
                    valueMap.put("stepList", stepList);
                    testcase.setTestSteps(testStep);
                }
            }

            Map<String, Map<String, Object>> testcaseNameValueMap = new HashMap<>();
            testcaseNameValueMap.put(testcase.getName(), valueMap);
            testcaseNameValueMapList.add(testcaseNameValueMap);
        }
        List<TCRCatalogTreeTestcase> tcrList =  createTestcasesWithoutDuplicate(treeIdTestcaseMap);
        Map<TCRCatalogTreeTestcase, Map<String, Object>> tcrTestcaseStatusMap = new HashMap<>();
        List<MapTestcaseToRequirement> mapTestcaseToRequirements = new ArrayList<>();
        loop1 : for (Map<String, Map<String, Object>> map : testcaseNameValueMapList) {
            for(Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                for(TCRCatalogTreeTestcase tcrCatalogTreeTestcase : tcrList) {
                    Long treeId = (Long) entry.getValue().get("treeId");
                    if (tcrCatalogTreeTestcase.getTestcase().getName().equals(entry.getKey()) && tcrCatalogTreeTestcase.getTcrCatalogTreeId().equals(treeId)) {
                        //same testcase, add id and status to map
                        Map<String, Object> statusMap = new HashMap<>();
                        if(entry.getValue().containsKey("statusId")) {
                            statusMap.put("statusId", entry.getValue().get("statusId"));
                        }

                        MapTestcaseToRequirement mapTestcaseToRequirement = (MapTestcaseToRequirement) entry.getValue().get("mapTestcaseToRequirement");
                        mapTestcaseToRequirement.setTestcaseId(tcrCatalogTreeTestcase.getTestcase().getId());
                        mapTestcaseToRequirements.add(mapTestcaseToRequirement);

                        if (entry.getValue().containsKey("attachments")) {
                            statusMap.put("attachments", entry.getValue().get("attachments"));
                        }

                        if (entry.getValue().containsKey("statusAttachment")) {
                            statusMap.put("statusAttachment", entry.getValue().get("statusAttachment"));
                        }

                        if (entry.getValue().containsKey("stepList")) {
                            statusMap.put("stepList", entry.getValue().get("stepList"));
                        }
                        tcrTestcaseStatusMap.put(tcrCatalogTreeTestcase, statusMap);
                        tcrList.remove(tcrCatalogTreeTestcase);
                        continue loop1;
                    }
                }
            }
        }
        List<String> msgs = requirementService.mapTestcaseToRequirements(mapTestcaseToRequirements);
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
            step = step.trim();
            if(keywordsList.contains(step.trim().split(" ", 2)[0])) {
                TestStepDetail testStepDetail = new TestStepDetail();
                Map<String, String> stepMap = new HashMap<String, String>();
                testStepDetail.setOrderId((long)i);
                testStepDetail.setStep(step.substring(0,step.indexOf("..")));
                stepMap.put("orderId", i.toString());
                stepMap.put("step", step.substring(0,step.indexOf("..")));

                String[] statusStringArr = step.split("\\.");
                status = statusStringArr[statusStringArr.length-1].trim();

                if(StringUtils.isNumeric(status.substring(0,1))) {
                    //first character is numeric, check item before this in array for status
                    status = statusStringArr[statusStringArr.length-2].trim();
                }

                if(status.startsWith("failed")) {
                    stepMap.put("status", "false");
                }else if(status.startsWith("skipped") || status.startsWith("undefined")) {
                    stepMap.put("status", "skipped");
                }else {
                    stepMap.put("status", "true");
                }
                stepsList.add(stepMap);
                testStep.getSteps().add(testStepDetail);
                i++;
            }
        }
        testStep.setMaxId((long)testStep.getSteps().size());
        return testStep;
    }

    private List<String> getTestcasesForEggplant(List<EggPlantResult> eggPlantResults) throws ParseException, ParserConfigurationException, SAXException, IOException {
        String scriptNameParseTemplate = "[{\"scriptName\": \"${testsuite:name}\"}]";
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

    private String getWorkspacePath() {
        return Jenkins.get().getWorkspaceFor(Jenkins.get().getItem(jenkinsProjectName)).toString();
    }

    private List<String> getAllIncludedFilePathList(String basePath, String includes) {
        FileSet fileSet = new FileSet();
        fileSet.setIncludes(includes);
        fileSet.setDir(new File(basePath));
        fileSet.setProject(new Project());

        List<String> filePathList = new ArrayList<>();
        for(String path : fileSet.getDirectoryScanner().getIncludedFiles()) {
            filePathList.add(basePath + File.separator + path);
        }
        return filePathList;
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

    public String getParserTemplateKey() {
        return parserTemplateKey;
    }

    public void setParserTemplateKey(String parserTemplateKey) {
        this.parserTemplateKey = parserTemplateKey;
    }
}
