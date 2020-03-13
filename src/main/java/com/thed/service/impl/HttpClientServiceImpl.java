package com.thed.service.impl;

import com.thed.service.HttpClientService;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by prashant on 18/6/19.
 */
public class HttpClientServiceImpl implements HttpClientService {

    private BasicCookieStore cookieStore; // This stores cookies for created by client or set by server side.
    private List<Header> headers;

    public HttpClientServiceImpl() {
        cookieStore = new BasicCookieStore();
        headers = new ArrayList<>();
    }

    @Override
    public List<Header> getHeaders() {
        return headers;
    }

    @Override
    public void addHeader(Header header) {
        this.headers.add(header);
    }

    @Override
    public BasicCookieStore getCookieStore() {
        return cookieStore;
    }

    @Override
    public void setCookieStore(BasicCookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        byte[] byteArray = buffer.toByteArray();

        return new String(byteArray, StandardCharsets.UTF_8);
    }

    @Override
    public String getRequest(String url) {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        try{
            HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
            HttpGet httpGet = new HttpGet(url);
            for (Header header:headers){
                httpGet.addHeader(header);
            }
            HttpResponse response = httpClient.execute(httpGet);
            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                return convertInputStreamToString(response.getEntity().getContent());
            }

        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    @Override
    public String postRequest(String url, String content) {
        StringEntity stringEntity = null;
        if(!StringUtils.isEmpty(content)) {
            stringEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
        }
        return postRequest(url, stringEntity);
    }

    @Override
    public String postRequest(String url, HttpEntity httpEntity) {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        try{
            HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
            HttpPost httpPost = new HttpPost(url);

            for (Header header:headers){
                httpPost.addHeader(header);
            }

//            if(!StringUtils.isEmpty(content)) {
//                StringEntity stringEntity = new StringEntity(content, contentType);
//                httpPost.setEntity(stringEntity);
//            }

            if(httpEntity != null) {
                httpPost.setEntity(httpEntity);
            }

            HttpResponse response = httpClient.execute(httpPost);
            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                return convertInputStreamToString(response.getEntity().getContent());
            }

        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String putRequest(String url, String content) {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        try{
            HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
            HttpPut httpPut = new HttpPut(url);

            for (Header header:headers){
                httpPut.addHeader(header);
            }

            if(!StringUtils.isEmpty(content)) {
                StringEntity stringEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
                httpPut.setEntity(stringEntity);
            }

            HttpResponse response = httpClient.execute(httpPut);
            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                return convertInputStreamToString(response.getEntity().getContent());
            }

        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void clear() {
        cookieStore.clear();
        headers.clear();
    }
}
