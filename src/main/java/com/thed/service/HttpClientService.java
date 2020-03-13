package com.thed.service;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.BasicCookieStore;

import java.util.List;

/**
 * Created by prashant on 18/6/19.
 */
public interface HttpClientService {

    String getRequest(String url);

    String postRequest(String url, String content);

    String postRequest(String url, HttpEntity httpEntity);

    String putRequest(String url, String content);

    BasicCookieStore getCookieStore();

    void setCookieStore(BasicCookieStore cookieStore);

    List<Header> getHeaders();

    void addHeader(Header header);

    void clear();

}
