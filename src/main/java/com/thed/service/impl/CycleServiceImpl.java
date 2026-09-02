package com.thed.service.impl;

import com.thed.model.*;
import com.thed.service.CycleService;
import com.thed.service.ExecutionService;
import com.thed.service.TCRCatalogTreeService;
import com.thed.service.TestcaseService;
import com.thed.utils.GsonUtil;
import com.thed.utils.ZephyrConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by prashant on 26/6/19.
 */
public class CycleServiceImpl extends BaseServiceImpl implements CycleService {

    private static final Logger log = Logger.getLogger(CycleServiceImpl.class);

    private ExecutionService executionService = new ExecutionServiceImpl();
    private TCRCatalogTreeService tcrCatalogTreeService = new TCRCatalogTreeServiceImpl();
    private TestcaseService testcaseService = new TestcaseServiceImpl();

    public CycleServiceImpl() {
        super();
    }

    @Override
    public List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException, IOException {
        return zephyrRestService.getAllCyclesForReleaseId(releaseId);
    }

    @Override
    public Cycle createCycle(Cycle cycle) throws URISyntaxException, IOException {
        return zephyrRestService.createCycle(cycle);
    }

    @Override
    public Cycle getCycleById(Long cycleId) throws URISyntaxException, IOException {
        return zephyrRestService.getCycleById(cycleId);
    }

    @Override
    public CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException, IOException {
        return zephyrRestService.createCyclePhase(cyclePhase);
    }

    @Override
    public Integer assignCyclePhaseToCreator(Long cyclePhaseId) throws URISyntaxException, IOException {
        return zephyrRestService.assignCyclePhaseToCreator(cyclePhaseId);
    }

    @Override
    public List<ReleaseTestSchedule> assignCyclePhaseToUser(CyclePhase cyclePhase, Long userId, Set<Long> additionalTreeIds) throws URISyntaxException, IOException {
        List<ReleaseTestSchedule> rtsList = new ArrayList<>();
        int batchSize = ZephyrConstants.BATCH_SIZE;

        Set<Long> treeIds = new LinkedHashSet<>();
        if (CollectionUtils.isNotEmpty(additionalTreeIds))
        {
            treeIds.addAll(additionalTreeIds);
        }
        treeIds.addAll(tcrCatalogTreeService.getTCRCatalogTreeIdHierarchy(cyclePhase.getTcrCatalogTreeId()));

        for(Long treeId : treeIds) {
            for(int pageNo = 0; true; pageNo++) {
                int offset = pageNo * batchSize;
                List<PlanningTestcase> planningTestcaseList = testcaseService.getTestcasesForTreeIdFromPlanning(treeId, offset, batchSize);
                if(planningTestcaseList.isEmpty()) {
                    //no testcases in this tree, skip it
                    break;
                }
                List<Long> tctIdList = planningTestcaseList.stream().map(planningTestcase -> planningTestcase.getTct().getId()).collect(Collectors.toList());
                rtsList.addAll(zephyrRestService.assignTCRCatalogTreeTestcasesToUser(cyclePhase.getId(), treeId, tctIdList, userId));

                if(planningTestcaseList.size() < batchSize) {
                    //no more testcases left in this tree to assign, move to next tree
                    break;
                }
            }
        }
        return rtsList;
    }

    @Override
    public Set<Long> addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, List<TCRCatalogTreeTestcase> testcases, Boolean includeHierarchy) throws URISyntaxException, IOException {
        //todo: this data parsing loop runs two times, once here and once in ZephyrRestService, need to fix this
        Map<Long, Set<Long>> treeTestcaseMap = new HashMap<>();
        Set<Long> discoveredTreeIds = new LinkedHashSet<>();

        int count = 0;
        for (TCRCatalogTreeTestcase testcase : testcases) {
            if(treeTestcaseMap.containsKey(testcase.getTcrCatalogTreeId())) {
                treeTestcaseMap.get(testcase.getTcrCatalogTreeId()).add(testcase.getTestcase().getId());
            }
            else {
                Set<Long> tctIds = new HashSet<>();
                tctIds.add(testcase.getTestcase().getId());
                treeTestcaseMap.put(testcase.getTcrCatalogTreeId(), tctIds);
            }
            count++;

            if(count == ZephyrConstants.BATCH_SIZE) {
                //batch limit reached, process these testcases
                String response = zephyrRestService.addTestcasesToFreeFormCyclePhase(cyclePhase, treeTestcaseMap, includeHierarchy);
                discoveredTreeIds.addAll(parseFrozenTreeIds(response));

                //testcases processed, clear map and reset count
                treeTestcaseMap = new HashMap<>();
                count = 0;
            }
        }

        if(!treeTestcaseMap.isEmpty()) {
            String response = zephyrRestService.addTestcasesToFreeFormCyclePhase(cyclePhase, treeTestcaseMap, includeHierarchy);
            discoveredTreeIds.addAll(parseFrozenTreeIds(response));
        }

        return discoveredTreeIds;
    }


    private Set<Long> parseFrozenTreeIds(String response) {
        Set<Long> ids = new LinkedHashSet<>();
        if (response == null || response.trim().isEmpty()) {
            return ids;
        }

        String xml = response;
        try {
            // Response body is typically a JSON string literal wrapping the XML document
            // (RestServiceUtil.toJsonStr(document.asXML())); unwrap it if so.
            String decoded = GsonUtil.CUSTOM_GSON.fromJson(response, String.class);
            if (decoded != null) {
                xml = decoded;
            }
        } catch (Exception e) {
            // not JSON-quoted, use the raw response as-is
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));
            NodeList nodeList = doc.getElementsByTagName("TCRCatalogTree");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String frozenId = ((Element) node).getAttribute("frozenId");
                    if (StringUtils.isNotBlank(frozenId)) {
                        try {
                            ids.add(Long.parseLong(frozenId));
                        } catch (NumberFormatException e) {
                            log.warn("parseFrozenTreeIds: skipping non-numeric frozenId=" + frozenId, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("parseFrozenTreeIds: failed to parse addTestcasesToFreeFormCyclePhase response as XML. response=" + response, e);
        }
        return ids;
    }
}
