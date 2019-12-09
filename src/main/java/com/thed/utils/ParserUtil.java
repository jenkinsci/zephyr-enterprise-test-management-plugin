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

/**
 * Created by prashant on 3/12/19.
 */
public class ParserUtil {

    Document getXMLDoc(String filePath) throws ParserConfigurationException, IOException, SAXException {
        File xmlFile = new File(filePath);
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
        document.getDocumentElement().normalize();

//        Element rootElement =  document.getDocumentElement();

        return document;
    }


    //entry method
    public List<Map> parseXmlLang(String filePath, String parseTemplate) throws IOException, SAXException, ParserConfigurationException {
        Document document = getXMLDoc(filePath);
        Map<String, Object> parseTemplateMap = new Gson().fromJson(parseTemplate, Map.class);
        List<String> deepLinks = getXmlPathDeepLinks(parseTemplateMap, new ArrayList<String>());
        List<Map> dataMapList = new ArrayList<Map>();
        for(String deepLink : deepLinks) {
            String[] xmlNodes = deepLink.split("\\.");
            fillMapWithXML(document, parseTemplateMap, deepCopy(parseTemplateMap), 0, xmlNodes, dataMapList);
        }
        return dataMapList;
    }

    List<String> getXmlPathDeepLinks(Map<String, Object> parseTemplateMap, List<String> deepLinks) {

        for (String key : parseTemplateMap.keySet()) {
            Object value = parseTemplateMap.get(key);
            if(value instanceof String) {
                if(((String) value).isEmpty()) {
                    continue;
                }
                String xmlPathVar = getXmlPathVar(String.valueOf(value));
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
            } else if(value instanceof Map) {
                getXmlPathDeepLinks((Map) value, deepLinks);
            }
        }

        return deepLinks;
    }

    //this method traverses xml nodes
    List<Map> fillMapWithXML(Node node, Map<String, Object> baseTemplateMap, Map<String, Object> dataMap, int currentLinkIndex, String[] xmlNodes, List<Map> dataMapList) {
        if (currentLinkIndex == xmlNodes.length) {
            //index reached size, no more nodes to dive in, so add the dataMap here as this is the end of recursion fellowship
            dataMapList.add(dataMap);
            return dataMapList;
        }
        String xmlVar = xmlNodes[currentLinkIndex];
        String xmlPath = joinString(".", currentLinkIndex + 1, xmlNodes);
        NodeList nodeList = null;
        if(node instanceof Document) {
            nodeList = ((Document) node).getElementsByTagName(xmlVar);
        } else {
            nodeList = ((Element) node).getElementsByTagName(xmlVar);
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            Map<String, Object> newDataMap = deepCopy(dataMap);
            fillMapWithElement(xmlPath, (Element) childNode, baseTemplateMap, newDataMap);
            fillMapWithXML(childNode, baseTemplateMap, newDataMap, currentLinkIndex + 1, xmlNodes, dataMapList);
        }
        return dataMapList;
    }

    //this method traverses data maps filling them with given element
    void fillMapWithElement(String tagPath, Element element, Map<String, Object> baseTemplateMap, Map dataMap) {
        for (String key : baseTemplateMap.keySet()) {
            Object value = baseTemplateMap.get(key);
            if(baseTemplateMap.get(key) instanceof Map) {
                // map found, also fill this with data
                //todo: this recursion should be controlled because if there is no relevant key any deeper than the recursion is useless
                fillMapWithElement(tagPath, element, (Map)baseTemplateMap.get(key), (Map)dataMap.get(key));
            } else {
                //check if this key-value uses any value from this element
                if(value.equals(tagPath)) {
                    dataMap.put(key, element.getNodeValue());
                } else if(value.toString().startsWith(tagPath + ":")) {
                    dataMap.put(key, element.getAttribute(getAttributeName(value.toString())));
                }
            }
        }
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
     * For xmlPath like 'testsuite.testcase:name', returns 'testsuite.testcase'
     * @param xmlPath
     * @return
     */
    String getXmlPathVar(String xmlPath) {
        String values[] = xmlPath.split(":");
        return values[0];
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
}
