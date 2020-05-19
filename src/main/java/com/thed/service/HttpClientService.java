package com.thed.service;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.List;

/**
 * Created by prashant on 18/6/19.
 */
public interface HttpClientService {

    String getRequest(String url) throws IOException;

    String postRequest(String url, String content) throws IOException;

    String postRequest(String url, HttpEntity httpEntity) throws IOException;

    String putRequest(String url, String content) throws IOException;

    BasicCookieStore getCookieStore();

    void setCookieStore(BasicCookieStore cookieStore);

    List<Header> getHeaders();

    void addHeader(Header header);

    void clear();

    void closeHttpClient() throws IOException;

}
