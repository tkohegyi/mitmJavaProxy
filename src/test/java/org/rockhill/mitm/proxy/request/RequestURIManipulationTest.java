package org.rockhill.mitm.proxy.request;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.rockhill.mitm.proxy.help.StubServerBase;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * This test checks if the request header can be accessed and altered by the request interceptors.
 * Headers are used to notify the request interceptor
 * No special header: no change in url - goes to webserver
 * 'A'->'A' forward to stubServer / http
 * 'B'->'B' forward to stubServer / https
 */
public class RequestURIManipulationTest extends StubServerBase {
    private static final String GET_REQUEST = "/anyUrl";
    private final Logger logger = LoggerFactory.getLogger(RequestURIManipulationTest.class);
    private HttpGet request;

    @Override
    protected void setUp2() {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        request = new HttpGet(GET_REQUEST);
    }

    @Override
    protected void tearDown2() {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        String headerValue;
        //normal server shall not be called if 'A' or 'B' header exists
        headerValue = request.getHeader("A");
        if (headerValue != null) {
            setLastException(new Exception("normal server shall not be called if 'A' header exists"));
        }
        headerValue = request.getHeader("B");
        if (headerValue != null) {
            setLastException(new Exception("normal server shall not be called if 'B' header exists"));
        }
    }

    @Override
    protected byte[] evaluateStubServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        String headerValue1;
        String headerValue2;
        //stub server shall be called if 'A' or 'B' header exists
        headerValue1 = request.getHeader("A");
        headerValue2 = request.getHeader("B");
        if (headerValue1 == null && headerValue2 == null) {
            setLastException(new Exception("stub server shall not be called if neither 'A' nor 'B' header exists"));
        }
        return null;
    }

    @Test
    public void simpleCallNoRedirect() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
            assertNull(getLastStubException());
            //check that answer is not changed
            assertEquals(SERVER_BACKEND, body);
        }
    }

    @Test
    public void simpleCallNoRedirectSecure() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
            assertNull(getLastStubException());
            //check that answer is not changed
            assertEquals(SERVER_BACKEND, body);
        }
    }

    @Test
    public void callHttp2HttpStub() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            request.addHeader("A", "A"); //header to be found -> redirect to sub / http
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
            assertNull(getLastStubException());
            //check that answer is not changed
            assertEquals(STUB_SERVER_BACKEND, body);
        }
    }

    @Test
    public void callHttp2SecureHttpStub() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            request.addHeader("B", "B"); //header to be found -> redirect to sub / http
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
            assertNull(getLastStubException());
            //check that answer is not changed
            assertEquals(STUB_SERVER_BACKEND, body);
        }
    }

    @Test
    public void callSecureHttp2HttpStub() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            request.addHeader("A", "A"); //header to be found -> redirect to sub / http
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
            assertNull(getLastStubException());
            //check that answer is not changed
            assertEquals(STUB_SERVER_BACKEND, body);
        }
    }

    @Test
    public void callSecureHttp2SecureHttpStub() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            request.addHeader("B", "B"); //header to be found -> redirect to sub / http
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
            assertNull(getLastStubException());
            //check that answer is not changed
            assertEquals(STUB_SERVER_BACKEND, body);
        }
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            Header header;
            //header to be found
            header = request.getMethod().getFirstHeader("A");
            if (header != null) {
                //redirect to stub / http
                try {
                    URI uri = new URI("http://127.0.0.1:" + getHttpStubPort() + "/stubUrl");
                    request.getMethod().setURI(uri);
                    logger.info("Request Interceptor Called - Redirect to STUB: {}", uri.toString());
                } catch (URISyntaxException e) {
                    setLastStubException(e);
                }
            }
            header = request.getMethod().getFirstHeader("B");
            if (header != null) {
                //redirect to stub / httpS
                try {
                    URI uri = new URI("https://127.0.0.1:" + getSecureStubPort() + "/stubUrl");
                    request.getMethod().setURI(uri);
                    logger.info("Request Interceptor Called - Redirect to STUB: {}", uri.toString());
                } catch (URISyntaxException e) {
                    setLastStubException(e);
                }
            }
        }
    }

}
