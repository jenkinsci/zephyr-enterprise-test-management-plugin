package com.thed.service;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Created by prashant on 18/6/19.
 */
public interface HttpClientService {

    String authenticationGetRequest(String url, String username, String password);

    String getRequest(String url);

    String postRequest(String url, String content);

    String postRequest(String url, HttpEntity httpEntity);

    String putRequest(String url, String content);

    void clear();

}
