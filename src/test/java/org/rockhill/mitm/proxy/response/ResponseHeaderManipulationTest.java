package org.rockhill.mitm.proxy.response;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.rockhill.mitm.proxy.ProxyServer;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This test checks if the response header can be accessed and altered by the response interceptors.
 * 3 header is in use/tested:
 * 'A'->'Aa' is to check header existence and accessibility
 * 'B'->'Bb' is altered to 'B'->'bBB' at request interceptor
 * 'C'->'Cc' is removed at request interceptor
 * + 1 header 'D'->'Dd' is added to response by response interceptor
 */
public class ResponseHeaderManipulationTest extends AnsweringServerBase {
    protected static final String GET_REQUEST = "/anyUrl";
    private final static Logger LOGGER = LoggerFactory.getLogger(ResponseHeaderManipulationTest.class);
    private HttpGet request;

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        ProxyServer.setResponseVolatile(true); //this ia a must !!!
        request = new HttpGet(GET_REQUEST);
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response) {
        //add test headers to the server response
        response.addHeader("A", "Aa"); //header to be found
        response.addHeader("B", "Bb"); //header to be altered
        response.addHeader("C", "Cc"); //header to be removed
    }

    @Test
    public void headerInterceptedAndAccessible() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
        //check headers in response
        Header h;
        //'A' existence
        h = response.getFirstHeader("A");
        assertNotNull("Cannot find 'A' header in response", h);
        //'B' changed
        h = response.getFirstHeader("B");
        assertEquals("bBB", h.getValue());
        //'C' must missing
        h = response.getFirstHeader("C");
        assertNull("Deleted header 'C' found", h);
        //'D' (the new header) must exist
        h = response.getFirstHeader("D");
        assertNotNull("Cannot find 'D' header in response", h);
    }

    @Test
    public void headerInterceptedAndAccessibleSecure() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(secureHost, request); //request is here
        httpClient.close();
        assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
        //check headers in response
        Header h;
        //'A' existence
        h = response.getFirstHeader("A");
        assertNotNull("Cannot find 'A' header in response", h);
        //'B' changed
        h = response.getFirstHeader("B");
        assertEquals("bBB", h.getValue());
        //'C' must missing
        h = response.getFirstHeader("C");
        assertNull("Deleted header 'C' found", h);
        //'D' (the new header) must exist
        h = response.getFirstHeader("D");
        assertNotNull("Cannot find 'D' header in response", h);
    }

    @Test
    public void headerInterceptedAndAccessibleButResponseIsNotVolatile() throws Exception {
        ProxyServer.setResponseVolatile(false); //interceptor shall not influence the response !
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
        //check headers in response
        Header h;
        //'A' existence
        h = response.getFirstHeader("A");
        assertNotNull("Cannot find 'A' header in response", h);
        //'B' unchanged
        h = response.getFirstHeader("B");
        assertEquals("Bb", h.getValue());
        //'C' must present
        h = response.getFirstHeader("C");
        assertNotNull("Header 'C' seems deleted", h);
        //'D' (the new header) must not exist
        h = response.getFirstHeader("D");
        assertNull("Shall not find 'D' header in response", h);
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            //nothing to do at request now
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            boolean found;

            //A header - just check that we can find it, do not alter
            String headerString = response.getHeader("A");
            assertEquals("'A' was not found at interceptor", headerString, "Aa");

            //B header - alter it
            found = false;
            Header[] headers = response.getHeaders();
            for (Header h : headers) {
                if (h.getName().equals("B")) {
                    LOGGER.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    found = true;
                    response.updateHeader(h, "bBB");
                }
            }
            assertTrue("'B' was not found at interceptor", found);

            //C header - remove it
            found = false;
            headers = response.getHeaders();
            for (Header h : headers) {
                if (h.getName().equals("C")) {
                    LOGGER.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    found = true;
                    response.removeHeader(h);
                }
            }
            assertTrue("'C' was not found at interceptor", found);

            //D header - just add this brand new header
            response.addHeader(new BasicHeader("D", "Dd"));
        }
    }
}
