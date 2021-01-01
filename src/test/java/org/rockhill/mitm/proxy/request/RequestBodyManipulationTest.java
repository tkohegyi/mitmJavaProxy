package org.rockhill.mitm.proxy.request;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This test checks if the request body can be accessed and altered by the request interceptors.
 */
public class RequestBodyManipulationTest extends AnsweringServerBase {
    private final static Logger LOGGER = LoggerFactory.getLogger(RequestBodyManipulationTest.class);
    private final static String REQ_BODY = "initial request body";

    protected static final String GET_REQUEST = "/anyUrl";
    private HttpPost request;

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        request = new HttpPost(GET_REQUEST);
        final StringEntity entity = new StringEntity(REQ_BODY, "UTF-8");
        entity.setChunked(true);
        request.setEntity(entity);
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response) {
        //check if body properly arrived to server
    }

    @Test
    public void headerInterceptedAndAccessible() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void headerInterceptedAndAccessibleSecure() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(secureHost, request); //request is here
        httpClient.close();
        assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            //get the body as string
            InputStream clonedInputStream = request.getPlayGround();
            try {
                String body = IOUtils.toString(clonedInputStream);
                clonedInputStream.reset();
                assertTrue("Cannot find the expected body", REQ_BODY.equals(body));

            } catch (IOException e) {
                setLastException(e);
            }
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
        }
    }
}
