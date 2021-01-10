package org.rockhill.mitm.proxy.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.rockhill.mitm.proxy.test.support.DefaultRequestInterceptor;
import org.rockhill.mitm.proxy.test.support.DefaultResponseInterceptor;
import org.rockhill.mitm.proxy.test.support.StubServerBase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;

/**
 * Tests the test client connection to test servers, both standard and stub servers, both get and put request types, both http and https types.
 * So altogether: 8 tests
 * All tests are WITHOUT using the proxy.
 * http/https * get/put * std server/stub server
 */
public class ClientToServerWithoutProxyTest extends StubServerBase {
    private static final String GET_REQUEST = "/anyUrl";
    private HttpGet requestGet;
    private HttpPost requestPost;
    private DefaultRequestInterceptor defaultRequestInterceptor;
    private DefaultResponseInterceptor defaultResponseInterceptor;


    @Override
    protected void setUpWithStub() {
        defaultRequestInterceptor = new DefaultRequestInterceptor();
        defaultResponseInterceptor = new DefaultResponseInterceptor();
        getProxyServer().addRequestInterceptor(defaultRequestInterceptor);
        getProxyServer().addResponseInterceptor(defaultResponseInterceptor);
        requestGet = new HttpGet(GET_REQUEST);
        requestPost = new HttpPost(GET_REQUEST);
        final StringEntity entity = new StringEntity("adsf", "UTF-8");
        entity.setChunked(true);
        requestPost.setEntity(entity);
    }

    @Override
    protected void tearDownWithStub() {
    }

    @Override
    protected void evaluateStubServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
    }

    @Test
    public void testSimpleGetRequest() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getHttpHost(), requestGet); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }

    @Test
    public void testSimpleGetRequestOverHTTPS() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getSecureHost(), requestGet); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }

    @Test
    public void testSimplePostRequest() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getHttpHost(), requestPost); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }

    @Test
    public void testSimplePostRequestOverHTTPS() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getSecureHost(), requestPost); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }

    @Test
    public void testSimpleGetRequestToStub() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getHttpStubHost(), requestGet); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", STUB_SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStub() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getSecureStubHost(), requestGet); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", STUB_SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }

    @Test
    public void testSimplePostRequestToStub() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getHttpStubHost(), requestPost); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", STUB_SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStub() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(WITHOUT_PROXY)) {
            HttpResponse response = httpClient.execute(getSecureStubHost(), requestPost); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", STUB_SERVER_BACKEND, body);
            assertEquals(0, defaultResponseInterceptor.getResponseCount());
            assertEquals(0, defaultRequestInterceptor.getRequestCount());
        }
    }
}