package org.rockhill.mitm.proxy.request;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.rockhill.mitm.proxy.ResponseInterceptor;
import org.rockhill.mitm.proxy.help.AnsweringServerBase;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This test checks if the request header can be accessed and altered by the request interceptors.
 * 3 header is in use/tested:
 * 'A'->'A' is to check header existence and accessibility
 * 'B'->'B' is altered to 'B'->'BB' at request interceptor
 * 'C'->'C' is removed at request interceptor
 * 'D'->'D' this header is added as new header at request interceptor
 */
public class RequestHeaderManipulationTest extends AnsweringServerBase {
    protected static final String GET_REQUEST = "/anyUrl";
    private final Logger logger = LoggerFactory.getLogger(RequestHeaderManipulationTest.class);
    private HttpGet request;

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        request = new HttpGet(GET_REQUEST);
        request.addHeader("A", "A"); //header to be found
        request.addHeader("B", "B"); //header to be altered
        request.addHeader("C", "C"); //header to be removed
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected byte[] evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        String headerValue;
        //check request header existence
        headerValue = request.getHeader("A");
        assertEquals("A", headerValue);
        //check altered header value
        headerValue = request.getHeader("B");
        assertEquals("BB", headerValue);
        //check deletion of header
        headerValue = request.getHeader("C");
        assertNull(headerValue);
        //check new header
        headerValue = request.getHeader("D");
        assertEquals("D", headerValue);
        return null;
    }

    @Test
    public void headerInterceptedAndAccessible() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void headerInterceptedAndAccessibleSecure() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
        httpClient.close();
        assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            Header header;
            //header to be found
            header = request.getMethod().getFirstHeader("A");
            assertEquals(header.getValue(), "A");
            //header to be altered
            header = request.getMethod().getFirstHeader("B");
            assertEquals(header.getValue(), "B");
            request.getMethod().removeHeader(header);
            request.getMethod().addHeader("B", "BB");
            //header to be removed
            header = request.getMethod().getFirstHeader("C");
            assertEquals(header.getValue(), "C");
            request.getMethod().removeHeader(header);
            //add brand new header
            request.getMethod().addHeader("D", "D");
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            Header[] headers = response.getRequestHeaders();
            boolean found;
            //header to be found
            found = false;
            for (Header h : headers) {
                if (h.getName().equals("A")) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    if (h.getValue().equals("A")) {
                        found = true;
                    }
                }
            }
            assertTrue("Cannot find request header 'A'->'A'", found);

            //header to be found as altered
            found = false;
            for (Header h : headers) {
                if (h.getName().equals("B")) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    if (h.getValue().equals("BB")) {
                        found = true;
                    }
                }
            }
            assertTrue("Cannot find altered request header 'B'->'BB'", found);

            //header not to be found
            found = false;
            for (Header h : headers) {
                if (h.getName().equals("C")) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    found = true;
                }
            }
            assertFalse("Shall not find deleted request header 'C'", found);
        }
    }
}
