package org.rockhill.mitm.proxy.request;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.rockhill.mitm.proxy.ResponseInterceptor;
import org.rockhill.mitm.proxy.help.StubServerBase;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This test checks if the request header can be accessed and altered by the request interceptors.
 * Headers are used to notify the request interceptor
 * No special header: no change in url - goes to webserver
 * 'A'->'A' forward to stubServer / http
 * 'B'->'B' forward to stubServer / https
 */
public class RequestURIManipulationTest extends StubServerBase {
    protected static final String GET_REQUEST = "/anyUrl";
    private HttpGet request;

    @Override
    protected void setUp2() {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        request = new HttpGet(GET_REQUEST);
    }

    @Override
    protected void tearDown2() {
    }

    @Override
    protected byte[] evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        String headerValue;
        //normal server shall not be called if 'A' or 'B' header exists
        headerValue = request.getHeader("A");
        assertNull(headerValue);
        headerValue = request.getHeader("B");
        assertNull(headerValue);
        return null;
    }

    @Override
    protected byte[] evaluateStubServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        String headerValue1, headerValue2;
        //stub server shall be called if 'A' or 'B' header exists
        headerValue1 = request.getHeader("A");
        headerValue2 = request.getHeader("B");
        assertTrue(headerValue1 != null || headerValue2 != null);
        return null;
    }

    @Test
    public void simpleCallNoRedirect() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        String body = EntityUtils.toString(response.getEntity());
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
        assertNull(getLastStubException());
        //check that answer is not changed
        assertEquals(SERVER_BACKEND, body);
    }

    @Test
    public void simpleCallNoRedirectSecure() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(secureHost, request); //request is here
        String body = EntityUtils.toString(response.getEntity());
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
        assertNull(getLastStubException());
        //check that answer is not changed
        assertEquals(SERVER_BACKEND, body);
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
                    URI uri = new URI("http://127.0.0.1:" + httpStubPort + "/stubUrl");
                    request.getMethod().setURI(uri);
                } catch (URISyntaxException e) {
                    setLastStubException(e);
                }
            }
            header = request.getMethod().getFirstHeader("A");
            if (header != null) {
                //redirect to stub / httpS
                try {
                    URI uri = new URI("https://127.0.0.1:" + secureStubPort + "/stubUrl");
                    request.getMethod().setURI(uri);
                } catch (URISyntaxException e) {
                    setLastStubException(e);
                }
            }
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            net.lightbody.bmp.proxy.jetty.util.URI uri = response.getProxyRequestURI();
            Header[] headers = response.getRequestHeaders();
            if (response.findHeader(headers, "A") != null) {
                //answer should come from stub / http
                assertEquals(httpStubPort, uri.getPort());
            }
            if (response.findHeader(headers, "B") != null) {
                //answer should come from stub / https
                assertEquals(secureStubPort, uri.getPort());
            }

        }
    }
}
