package com.thed.utils;

import com.google.gson.Gson;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by prashant on 3/12/19.
 */
public class ParserUtil {

    public static final String parserVariablePattern = "\\$[-.:\\w]*[^.:$\\s]";

    public Document getXMLDoc(String filePath) throws ParserConfigurationException, IOException, SAXException {
        File xmlFile = new File(filePath);
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
        document.getDocumentElement().normalize();
        return document;
    }

    public List<Map> parseXmlLang(String filePath, String parseTemplate) throws IOException, SAXException, ParserConfigurationException {
        Document document = getXMLDoc(filePath);
        List<Map<String, Object>> parseTemplateMapList = new Gson().fromJson(parseTemplate, List.class);
        XMLLink rootXMLLink = new XMLLink();
        getXmlPathDeepLinks(rootXMLLink, parseTemplateMapList.get(0), new ArrayList<String>(), XMLLink.Type.ARRAY);
        List<Map> dataMapList = new ArrayList<Map>();
        startParsing(rootXMLLink, document, parseTemplateMapList.get(0), deepCopy(parseTemplateMapList.get(0)), dataMapList, false);
        return dataMapList;
    }

    void getXmlPathDeepLinks(XMLLink xmlLink, Map<String, Object> parseTemplateMap, List<String> deepLinks, XMLLink.Type type) {
        for (String key : parseTemplateMap.keySet()) {
            Object value = parseTemplateMap.get(key);
            if(value instanceof String) {
                if(((String) value).isEmpty()) {
                    continue;
                }
                List<String> xmlPathVarList = getVariablesFromString(String.valueOf(value));
                for (String xmlPathVarStr : xmlPathVarList) {
                    String xmlPathVar = getXmlPathVar(String.valueOf(xmlPathVarStr));
                    xmlLink.attachLink(xmlPathVar.split("\\."), type);
                    if(deepLinks.size() == 0) {
                        deepLinks.add(xmlPathVar);
                    } else {
                        for (int i = 0; i < deepLinks.size(); i++) {
                            if(xmlPathVar.startsWith(deepLinks.get(i)) && deepLinks.get(i).length() < xmlPathVar.length()) {
                                //new xmlPathVar starts with already present
                                deepLinks.set(i, xmlPathVar);
                            }
                        }
                    }
                }
            } else if(value instanceof Map) {
                getXmlPathDeepLinks(xmlLink, (Map) value, deepLinks, XMLLink.Type.OBJECT);
            } else if(value instanceof List) {
                List valueList = (List) value;
                for (Object v : valueList) {
                    getXmlPathDeepLinks(xmlLink, (Map) v, deepLinks, XMLLink.Type.ARRAY);
                }
            }
        }
    }

    //this method traverses xml nodes
    boolean startParsing(XMLLink xmlLink, Node node, Map<String, Object> baseTemplateMap, Map<String, Object> dataMap, List<Map> dataMapList, boolean changed) {
        NodeList nodeList = null;
        if(node != null) {
            if(node instanceof Document) {
                nodeList = ((Document) node).getElementsByTagName(xmlLink.getXmlTagName());
            } else {
                nodeList = ((Element) node).getElementsByTagName(xmlLink.getXmlTagName());
            }
        }

        if(node == null || nodeList.getLength() == 0) {
            Map<String, Object> newDataMap = deepCopy(dataMap);
            boolean aChanged = startFillingMap(xmlLink, null, baseTemplateMap, newDataMap, changed);
//            if(changed || aChanged) {
//                dataMapList.add(newDataMap);
//            }

            List<XMLLink> objectChildLinks = xmlLink.getChildWithType(XMLLink.Type.OBJECT);

            if(objectChildLinks.isEmpty() && (changed || aChanged)) {
                //no more kids to traverse, add the newDataMap to list
                dataMapList.add(newDataMap);
                return true;
            }

            for (XMLLink link : objectChildLinks) {
                aChanged = startParsing(link, node, baseTemplateMap, newDataMap, dataMapList, changed);
                changed = aChanged || changed;
            }
        } else {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node childNode = nodeList.item(i);
                Map<String, Object> newDataMap = deepCopy(dataMap);
                changed = startFillingMap(xmlLink, (Element) childNode, baseTemplateMap, newDataMap, changed);

                List<XMLLink> objectChildLinks = xmlLink.getChildWithType(XMLLink.Type.OBJECT);

                if(objectChildLinks.isEmpty() && changed) {
                    //no more kids to traverse, add the newDataMap to list
                    dataMapList.add(newDataMap);
                    continue;
                }

                List<Map> newDataMapList = new ArrayList<Map>();
                newDataMapList.add(newDataMap);

                for (XMLLink link : objectChildLinks) {

                    List<Map> iterationList = deepyCopy(newDataMapList);

                    Map<Map, List<Map>> changeTrackMap = new HashMap<Map, List<Map>>();

                    for(Map currentDataMap : iterationList) {
                        List<Map> carryList = new ArrayList<Map>();
                        boolean aChange = startParsing(link, childNode, baseTemplateMap, currentDataMap, carryList, changed);
                        changed = aChange || changed;

                        if(!carryList.isEmpty()) {
                            changeTrackMap.put(currentDataMap, deepyCopy(carryList));
                        } else if(changed) {
                            changeTrackMap.put(currentDataMap, null);
                        }
                    }

                    newDataMapList = new ArrayList<Map>();

                    for (Map.Entry<Map, List<Map>> entry : changeTrackMap.entrySet()) {
                        if(entry.getValue() == null) {
                            newDataMapList.add(entry.getKey());
                        } else {
                            newDataMapList.addAll(entry.getValue());
                        }
                    }
                }

                if(changed) {
                    dataMapList.addAll(newDataMapList);
                }

            }
        }
        return changed;
    }

    //this method traverses data maps filling them with given element
    boolean startFillingMap(XMLLink xmlLink, Element element, Map<String, Object> baseTemplateMap, Map<String, Object> dataMap, boolean changed) {
        for (String key : dataMap.keySet()) {
            Object value = dataMap.get(key);
            if(value instanceof Map) {
                // map found, also fill this with data
                //todo: this recursion should be controlled because if there is no relevant key any deeper than the recursion is useless
                changed = startFillingMap(xmlLink, element, (Map) baseTemplateMap.get(key), (Map) dataMap.get(key), changed);
            } else if(value instanceof String) {

                List<String> xmlPathVarList = getVariablesFromString(value.toString());
                String resultValue = value.toString();
                for (String xmlPathVar : xmlPathVarList) {
                    if(xmlPathVar.equals("$"+xmlLink.getXMLPath())) {
                        if(element == null) {
                            resultValue = resultValue.replace(xmlPathVar, "");
                        } else {
                            resultValue = resultValue.replace(xmlPathVar, element.getTextContent());
                            changed = true;
                        }

                    } else if(xmlPathVar.startsWith("$" + xmlLink.getXMLPath() + ":")) {
                        if(element == null) {
                            resultValue = resultValue.replace(xmlPathVar, "");
                        } else {
                            resultValue = resultValue.replace(xmlPathVar, element.getAttribute(getAttributeName(xmlPathVar)));
                            changed = true;
                        }

                    }
                }
                dataMap.put(key, resultValue);
            }
            else if(value instanceof List) {
                List valueList = (List) baseTemplateMap.get(key);
                List<Map> dataMapList = new ArrayList<Map>();
                Map valueDataMap = (Map) valueList.get(0);

                if(element != null) {
                    List<XMLLink> linkList = xmlLink.getChildWithType(XMLLink.Type.ARRAY);
                    for (XMLLink link : linkList) {
//                        List<Map> newDataMapList = new ArrayList<Map>();
                        boolean aChanged = startParsing(link, element, deepCopy(valueDataMap), deepCopy(valueDataMap), dataMapList, false);
                        if(!aChanged) {
//                            dataMapList.add(valueDataMap);
                        }
                    }
                    if(linkList.size() != 0) {
                        dataMap.put(key, dataMapList);
                    }
                } else {
                    List<XMLLink> linkList = xmlLink.getChildWithType(XMLLink.Type.ARRAY);
                    if(linkList.size() != 0) {
                        dataMap.put(key, dataMapList);
                    }
                }
            }
        }
        return changed;
    }

    /**
     * For xmlPath like 'testsuite.testcase:name', returns 'name'
     * @param xmlPath
     * @return
     */
    String getAttributeName(String xmlPath) {
        String values[] = xmlPath.split(":");
        if(values.length == 1) {
            return null;
        }
        return values[values.length - 1];
    }

    /**
     * For xmlPath like '$testsuite.testcase:name', returns 'testsuite.testcase'
     * @param xmlPath
     * @return
     */
    String getXmlPathVar(String xmlPath) {
        String values[] = xmlPath.split(":");
        return values[0].replace("$", "");
    }


    String joinString(String joinChar, int length, String[] list) {
        String value = list[0];
        for (int i = 1; i < length; i++) {
            value += joinChar + list[i];
        }
        return value;
    }

    Map deepCopy(Map originalMap) {
        Gson gson = new Gson();
        String json = gson.toJson(originalMap);
        Map cloneMap = gson.fromJson(json, Map.class);
        return cloneMap;
    }

    List deepyCopy(List originalList) {
        Gson gson = new Gson();
        String json = gson.toJson(originalList);
        List cloneList = gson.fromJson(json, List.class);
        return cloneList;
    }

    List<String> getVariablesFromString(String value) {
        Pattern pattern = Pattern.compile(parserVariablePattern);
        Matcher matcher = pattern.matcher(value);
        List<String> variables = new ArrayList<String>();
        while (matcher.find()) {
            variables.add(matcher.group());
        }
        return variables;
    }
}
